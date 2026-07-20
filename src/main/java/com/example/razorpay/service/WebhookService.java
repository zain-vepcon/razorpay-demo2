package com.example.razorpay.service;

import org.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.WebhookEvent;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

	private final WebhookEventRepository webhookEventRepository;
	private final PaymentOrderRepository paymentOrderRepository;
	private final FulfillmentService fulfillmentService;

	/**
	 * Returns true if this event_id has already been processed (Razorpay retries
	 * webhooks on non-2xx responses or timeouts, so the same event can legitimately
	 * arrive more than once).
	 */
	public boolean isDuplicate(String eventId) {
		return eventId != null && webhookEventRepository.findByEventId(eventId).isPresent();
	}

	public void recordEvent(String eventId, String eventType, String rawPayload, boolean signatureValid) {
		WebhookEvent event = new WebhookEvent();
		event.setEventId(eventId);
		event.setEventType(eventType);
		event.setRawPayload(rawPayload);
		event.setSignatureValid(signatureValid);
		webhookEventRepository.save(event);
		log.info("Webhook {} received eventId={}", eventType, eventId);
	}

	/**
	 * Routes each Razorpay event type to the relevant business logic. Full event
	 * list: https://razorpay.com/docs/webhooks/payloads/payments/
	 */

	@Async("webhookExecutor")
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

			log.error("Webhook processing failed", ex);

			log.info("Processing {} on thread {}", eventType, Thread.currentThread().getName());

		}

	}

	@Transactional
	protected void handlePaymentCaptured(JSONObject payload) {

		JSONObject payment = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = payment.getString("order_id");
		String paymentId = payment.getString("id");

		paymentOrderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {

			order.setStatus(PaymentStatus.PAID);
			order.setRazorpayPaymentId(paymentId);

			order.setFailureReason(null);
			order.setFailureCode(null);
			order.setFailureSource(null);
			order.setFailureStep(null);

			paymentOrderRepository.save(order);
			fulfillmentService.fulfillOrder(order);

			try {

				Thread.sleep(10000);

			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();

			}

//			log.info("Payment captured for order {}", orderId);
			log.info("Thread={} Event={} Order={} Payment={}", Thread.currentThread().getName(), "payment.captured",
					orderId, paymentId);
		});
	}

	private void handlePaymentFailed(JSONObject payload) {

		JSONObject payment = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = payment.getString("order_id");

		String paymentId = payment.getString("id");

		String reason = payment.optString("error_description", "Unknown");

		updateOrderStatus(orderId, paymentId, PaymentStatus.FAILED, reason);
		log.error("Payment failed order={} reason={}", orderId, reason);

	}

	private void handleOrderPaid(JSONObject payload) {

		JSONObject order = payload.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");

		String orderId = order.getString("id");

		log.info("Order {} is fully paid.", orderId);

	}

	private void updateOrderStatus(String razorpayOrderId, String paymentId, PaymentStatus status,
			String failureReason) {
		if (razorpayOrderId == null)
			return;
		paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresentOrElse(order -> {
			order.setStatus(status);
			order.setRazorpayPaymentId(paymentId);
			order.setFailureReason(failureReason);
			paymentOrderRepository.save(order);
			log.info("Order {} updated to {} paymentId={}", razorpayOrderId, status, paymentId);

		}, () -> log.warn("Webhook referenced unknown order_id={} (not created via this service)", razorpayOrderId));

	}

	private void handlePaymentAuthorized(JSONObject payload) {

		JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = paymentEntity.optString("order_id");
		String paymentId = paymentEntity.optString("id");

		updateOrderStatus(orderId, paymentId, PaymentStatus.AUTHORIZED, null);

		log.info("Order {} updated to AUTHORIZED", orderId);
	}
}
