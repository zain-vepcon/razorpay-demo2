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

/**
 * Service responsible for handling Razorpay webhook events.
 * 
 * Responsibilities: - Deduplication of events (Razorpay retries webhooks on
 * failures). - Recording raw webhook payloads for audit/debugging. - Routing
 * events to appropriate business logic handlers. - Updating payment order
 * status in the database. - Triggering fulfillment workflows after successful
 * payment capture.
 * 
 * * * * @author Zain * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

	private final WebhookEventRepository webhookEventRepository;
	private final PaymentOrderRepository paymentOrderRepository;
	private final FulfillmentService fulfillmentService;

	/**
	 * Checks if the given eventId has already been processed. Razorpay may retry
	 * sending the same webhook event multiple times.
	 *
	 * @param eventId Razorpay event ID
	 * @return true if event already exists in DB
	 */
	public boolean isDuplicate(String eventId) {
		return eventId != null && webhookEventRepository.findByEventId(eventId).isPresent();
	}

	/**
	 * Records the webhook event in the database for auditing.
	 *
	 * @param eventId        Razorpay event ID
	 * @param eventType      Type of event (e.g., payment.captured)
	 * @param rawPayload     Raw JSON payload
	 * @param signatureValid Whether the webhook signature was valid
	 */
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
	 * Routes Razorpay webhook events to appropriate handlers. Full event list:
	 * https://razorpay.com/docs/webhooks/payloads/payments/
	 *
	 * @param eventType Event type string
	 * @param payload   JSON payload
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

	/**
	 * Handles payment.captured event. Updates order status to PAID and triggers
	 * fulfillment.
	 */
	@Transactional
	protected void handlePaymentCaptured(JSONObject payload) {
		JSONObject payment = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = payment.getString("order_id");
		String paymentId = payment.getString("id");

		paymentOrderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {
			order.setStatus(PaymentStatus.PAID);
			order.setRazorpayPaymentId(paymentId);

			// Clear failure details if previously set
			order.setFailureReason(null);
			order.setFailureCode(null);
			order.setFailureSource(null);
			order.setFailureStep(null);

			paymentOrderRepository.save(order);
			fulfillmentService.fulfillOrder(order);

			try {
				Thread.sleep(10000); // simulate downstream delay
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			log.info("Thread={} Event={} Order={} Payment={}", Thread.currentThread().getName(), "payment.captured",
					orderId, paymentId);
		});
	}

	/**
	 * Handles payment.failed event. Updates order status to FAILED with reason.
	 */
	private void handlePaymentFailed(JSONObject payload) {
		JSONObject payment = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = payment.getString("order_id");
		String paymentId = payment.getString("id");
		String reason = payment.optString("error_description", "Unknown");

		updateOrderStatus(orderId, paymentId, PaymentStatus.FAILED, reason);
		log.error("Payment failed order={} reason={}", orderId, reason);
	}

	/**
	 * Handles order.paid event. Logs that the order is fully paid.
	 */
	private void handleOrderPaid(JSONObject payload) {
		JSONObject order = payload.getJSONObject("payload").getJSONObject("order").getJSONObject("entity");
		String orderId = order.getString("id");
		log.info("Order {} is fully paid.", orderId);
	}

	/**
	 * Updates order status in DB.
	 *
	 * @param razorpayOrderId Razorpay order ID
	 * @param paymentId       Razorpay payment ID
	 * @param status          Payment status
	 * @param failureReason   Failure reason (if any)
	 */
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

	/**
	 * Handles payment.authorized event. Updates order status to AUTHORIZED.
	 */
	private void handlePaymentAuthorized(JSONObject payload) {
		JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");

		String orderId = paymentEntity.optString("order_id");
		String paymentId = paymentEntity.optString("id");

		updateOrderStatus(orderId, paymentId, PaymentStatus.AUTHORIZED, null);
		log.info("Order {} updated to AUTHORIZED", orderId);
	}
}
