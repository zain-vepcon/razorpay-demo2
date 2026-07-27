package com.example.razorpay.service.Impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.WebhookEvent;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.repository.WebhookEventRepository;
import com.example.razorpay.service.FulfillmentService;
import com.example.razorpay.service.KafkaProducerService;
import com.example.razorpay.service.WebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link WebhookService}.
 *
 * <p>
 * Handles Razorpay webhook events by updating payment orders, recording events,
 * and triggering fulfillment workflows.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

	private final WebhookEventRepository webhookEventRepository;
	private final PaymentOrderRepository paymentOrderRepository;
	private final FulfillmentService fulfillmentService;
	private final KafkaProducerService kafkaProducerService;
	private static final long FULFILLMENT_DELAY = 10000;

	@Override
	public boolean isDuplicate(String eventId) {
		return eventId != null && webhookEventRepository.findByEventId(eventId).isPresent();
	}

	@Override
	public void recordEvent(String eventId, String eventType, String rawPayload, boolean signatureValid,
			JSONObject payload) {

		WebhookEvent event = new WebhookEvent();
		event.setEventId(eventId);
		event.setEventType(eventType);
		event.setRawPayload(rawPayload);
		event.setSignatureValid(signatureValid);
		LocalDateTime createdAt;

		if (payload.has("created_at")) {

			long createdAtEpoch = payload.getLong("created_at");

			createdAt = Instant.ofEpochSecond(createdAtEpoch).atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();

		} else {

			createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

		}

		event.setCreatedAt(createdAt);

		webhookEventRepository.save(event);

		log.info("Webhook {} received eventId={}", eventType, eventId);
	}

	@Override
	@Transactional
	public void process(String eventType, JSONObject payload) {
		try {
			switch (eventType) {
			case "payment.authorized" -> handlePaymentAuthorized(payload);
			case "payment.captured" -> handlePaymentCaptured(payload);
			case "payment.failed" -> handlePaymentFailed(payload);
			case "order.paid" -> handleOrderPaid(payload);
			case "refund.created" -> log.info("Refund created");
			case "refund.processed" -> log.info("Refund processed");
			default -> log.info("Unhandled webhook event: {}", eventType);
			}
		} catch (Exception ex) {
			log.error("Webhook processing failed: {}", ex.getMessage());
			log.info("Processing {} on thread {}", eventType, Thread.currentThread().getName());
		}
	}

	@CacheEvict(value = { "paymentById", "paymentByOrderId", "paymentByPaymentId", "allPayments" }, allEntries = true)
	@Transactional
	protected void handlePaymentCaptured(JSONObject payload) {

		JSONObject payment = getPaymentEntity(payload);

		if (payment == null) {
			log.warn("Invalid payment.captured payload");
			return;
		}

		long createdAtEpoch = payment.optLong("created_at", Instant.now().getEpochSecond());

		String orderId = payment.getString("order_id");
		String paymentId = payment.getString("id");

		paymentOrderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {

			order.setStatus(PaymentStatus.PAID);
			order.setRazorpayPaymentId(paymentId);
			LocalDateTime paymentCreatedAt = Instant.ofEpochSecond(createdAtEpoch).atZone(ZoneId.of("Asia/Kolkata"))
					.toLocalDateTime();

			order.setPaymentCreatedAt(paymentCreatedAt);

			order.setFailureReason(null);

			paymentOrderRepository.save(order);

			PaymentEvent event = PaymentEvent.builder().eventType("PAYMENT_SUCCESS").orderId(orderId)
					.paymentId(paymentId).amount(order.getAmount()).receipt(order.getReceipt())
					.currency(order.getCurrency()).status(PaymentStatus.PAID).timestamp(System.currentTimeMillis())
					.build();

			kafkaProducerService.publish(event);

			fulfillmentService.fulfillOrder(order);
			try {
				Thread.sleep(FULFILLMENT_DELAY);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			log.info("Thread={} Event={} Order={} Payment={}", Thread.currentThread().getName(), "payment.captured",
					orderId, paymentId);
		});

	}

	private void handlePaymentFailed(JSONObject payload) {
		JSONObject payment = getPaymentEntity(payload);
		if (payment == null) {
			log.warn("Invalid payment.failed webhook payload");
			return;
		}

		String orderId = payment.optString("order_id");
		String paymentId = payment.optString("id");
		String reason = payment.optString("error_description", "Unknown");

		updateOrderStatus(orderId, paymentId, PaymentStatus.FAILED, reason);
		log.error("Payment failed order={} reason={}", orderId, reason);
	}

	private JSONObject getPaymentEntity(JSONObject payload) {
		JSONObject webhookPayload = payload.optJSONObject("payload");
		if (webhookPayload == null)
			return null;

		JSONObject payment = webhookPayload.optJSONObject("payment");
		if (payment == null)
			return null;

		return payment.optJSONObject("entity");
	}

	private void handleOrderPaid(JSONObject payload) {

		JSONObject order = payload.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");

		String orderId = order.getString("id");

		log.info("Order {} is fully paid.", orderId);
	}

	private void updateOrderStatus(String razorpayOrderId, String paymentId, PaymentStatus status,
			String failureReason) {

		if (razorpayOrderId == null) {
			return;
		}

		paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresentOrElse(order -> {

			if (status == PaymentStatus.FAILED && order.getStatus() == PaymentStatus.FAILED) {

				log.info("Failure already processed for Order={}", razorpayOrderId);
				return;
			}

			order.setStatus(status);
			order.setRazorpayPaymentId(paymentId);
			order.setFailureReason(failureReason);

			paymentOrderRepository.save(order);
			if (status == PaymentStatus.FAILED) {
				fulfillmentService.handlePaymentFailure(order);
			}

			PaymentEvent event = PaymentEvent.builder().eventType(status.name()).orderId(order.getRazorpayOrderId())
					.paymentId(order.getRazorpayPaymentId()).amount(order.getAmount()).receipt(order.getReceipt())
					.currency(order.getCurrency() != null ? order.getCurrency() : "INR").status(status)
					.failureReason(failureReason).timestamp(System.currentTimeMillis()).build();

			kafkaProducerService.publish(event);

			log.info("Kafka Event Published : Order={} Status={}", order.getRazorpayOrderId(), status);

		}, () -> log.warn("Order not found : {}", razorpayOrderId));
	}

	private void handlePaymentAuthorized(JSONObject payload) {
		JSONObject paymentEntity = getPaymentEntity(payload);
		if (paymentEntity == null) {
			log.warn("Invalid payment.authorized payload");
			return;
		}

		String orderId = paymentEntity.optString("order_id");
		String paymentId = paymentEntity.optString("id");

		updateOrderStatus(orderId, paymentId, PaymentStatus.AUTHORIZED, null);
		log.info("Order {} updated to AUTHORIZED", orderId);
	}
}
