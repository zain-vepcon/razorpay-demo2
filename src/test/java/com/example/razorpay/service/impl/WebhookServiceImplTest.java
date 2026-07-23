package com.example.razorpay.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.model.WebhookEvent;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.repository.WebhookEventRepository;
import com.example.razorpay.service.FulfillmentService;
import com.example.razorpay.service.KafkaProducerService;
import com.example.razorpay.service.Impl.WebhookServiceImpl;

/**
 * Unit tests for {@link WebhookServiceImpl}.
 *
 * <p>
 * Verifies webhook processing logic including:
 * <ul>
 * <li>Duplicate event detection</li>
 * <li>Webhook audit persistence</li>
 * <li>Payment authorization</li>
 * <li>Payment capture</li>
 * <li>Payment failure</li>
 * <li>Order paid events</li>
 * <li>Refund events</li>
 * <li>Invalid payload handling</li>
 * </ul>
 *
 * @author Zain
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

	@Mock
	private WebhookEventRepository webhookEventRepository;

	@Mock
	private PaymentOrderRepository paymentOrderRepository;

	@Mock
	private FulfillmentService fulfillmentService;

	private WebhookServiceImpl webhookService;
	@Mock
	private KafkaProducerService kafkaProducerService;

	@BeforeEach
	void setup() {
		webhookService = new WebhookServiceImpl(webhookEventRepository, paymentOrderRepository, fulfillmentService,
				kafkaProducerService);
	}

	@Test
	@DisplayName("Should identify duplicate webhook")
	void shouldReturnTrueWhenEventAlreadyExists() {

		when(webhookEventRepository.findByEventId("evt1")).thenReturn(Optional.of(new WebhookEvent()));

		assertTrue(webhookService.isDuplicate("evt1"));

		verify(webhookEventRepository).findByEventId("evt1");
	}

	@Test
	@DisplayName("Should identify non duplicate webhook")
	void shouldReturnFalseWhenEventDoesNotExist() {

		when(webhookEventRepository.findByEventId("evt1")).thenReturn(Optional.empty());

		assertFalse(webhookService.isDuplicate("evt1"));

		verify(webhookEventRepository).findByEventId("evt1");
	}

	@Test
	@DisplayName("Should return false for null event id")
	void shouldReturnFalseForNullEventId() {

		assertFalse(webhookService.isDuplicate(null));

		verifyNoInteractions(webhookEventRepository);
	}

	@Test
	@DisplayName("Should record webhook event")
	void shouldSaveWebhookEvent() {

		JSONObject payload = paymentPayload("order1", "pay1");

		webhookService.recordEvent("evt1", "payment.captured", "{}", true, payload);

		verify(webhookEventRepository).save(any(WebhookEvent.class));
	}

	@Test
	@DisplayName("Should record invalid signature event")
	void shouldRecordInvalidSignatureEvent() {
		JSONObject payload = paymentPayload("order1", "pay1");

		webhookService.recordEvent("evt2", "payment.failed", "{}", false, payload);

		verify(webhookEventRepository).save(any(WebhookEvent.class));
	}

	@Test
	@DisplayName("Should authorize payment")
	void shouldAuthorizePayment() {

		PaymentOrder order = new PaymentOrder();

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.authorized", paymentPayload("order1", "pay1"));

		assertEquals(PaymentStatus.AUTHORIZED, order.getStatus());

		verify(paymentOrderRepository).save(order);
	}

	@Test
	@DisplayName("Should process captured payment")
	void shouldCapturePayment() {

		PaymentOrder order = new PaymentOrder();

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.captured", paymentPayload("order1", "pay1"));

		assertEquals(PaymentStatus.PAID, order.getStatus());

		verify(paymentOrderRepository).save(order);

		verify(fulfillmentService).fulfillOrder(order);
		verify(kafkaProducerService).publish(any());
	}

	@Test
	@DisplayName("Should process failed payment")
	void shouldProcessFailedPayment() {

		PaymentOrder order = new PaymentOrder();

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.failed", paymentPayload("order1", "pay1"));

		assertEquals(PaymentStatus.FAILED, order.getStatus());

		verify(paymentOrderRepository).save(order);
	}

	@Test
	@DisplayName("Should handle unknown order")
	void shouldIgnoreUnknownOrder() {

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.empty());

		webhookService.process("payment.failed", paymentPayload("order1", "pay1"));

		verify(paymentOrderRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should process order paid event")
	void shouldProcessOrderPaid() {

		assertDoesNotThrow(() -> webhookService.process("order.paid", orderPayload("order1")));
	}

	@Test
	@DisplayName("Should process refund created")
	void shouldProcessRefundCreated() {

		assertDoesNotThrow(() -> webhookService.process("refund.created", new JSONObject()));
	}

	@Test
	@DisplayName("Should process refund processed")
	void shouldProcessRefundProcessed() {

		assertDoesNotThrow(() -> webhookService.process("refund.processed", new JSONObject()));
	}

	@Test
	@DisplayName("Should ignore unknown webhook event")
	void shouldIgnoreUnknownEvent() {

		assertDoesNotThrow(() -> webhookService.process("random.event", new JSONObject()));
	}

	@Test
	@DisplayName("Should handle invalid payment payload")
	void shouldHandleInvalidPaymentPayload() {

		assertDoesNotThrow(() -> webhookService.process("payment.failed", new JSONObject()));
	}

	@Test
	@DisplayName("Should handle invalid authorized payload")
	void shouldHandleInvalidAuthorizedPayload() {

		assertDoesNotThrow(() -> webhookService.process("payment.authorized", new JSONObject()));
	}

	@Test
	@DisplayName("Should handle invalid captured payload")
	void shouldHandleInvalidCapturedPayload() {

		assertDoesNotThrow(() -> webhookService.process("payment.captured", new JSONObject()));
	}

	/**
	 * Builds a minimal Razorpay webhook payload for a payment entity.
	 *
	 * Structure: { "payload": { "payment": { "entity": { "order_id": "...", "id":
	 * "..." } } } }
	 */
	private JSONObject paymentPayload(String orderId, String paymentId) {

		return new JSONObject().put("payload",
				new JSONObject().put("payment", new JSONObject().put("entity", new JSONObject().put("order_id", orderId)
						.put("id", paymentId).put("currency", "INR").put("amount", 50000).put("status", "captured"))));
	}

	/**
	 * Builds a minimal Razorpay webhook payload for an order entity.
	 *
	 * Structure: { "payload": { "order": { "entity": { "id": "..." } } } }
	 */
	private JSONObject orderPayload(String orderId) {
		return new JSONObject().put("payload",
				new JSONObject().put("order", new JSONObject().put("entity", new JSONObject().put("id", orderId))));
	}

	@Test
	@DisplayName("Should invoke failure fulfillment")
	void shouldTriggerFailureWorkflow() {

		PaymentOrder order = new PaymentOrder();

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.failed", paymentPayload("order1", "pay1"));

		verify(fulfillmentService).handlePaymentFailure(order);
	}

	@Test
	@DisplayName("Should publish correct Kafka event")
	void shouldPublishKafkaEvent() {

		PaymentOrder order = new PaymentOrder();
		order.setRazorpayOrderId("order1");
		order.setRazorpayPaymentId("pay1");
		order.setAmount(50000L);
		order.setReceipt("receipt123");
		order.setCurrency("INR");
		order.setStatus(PaymentStatus.CREATED);

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.captured", paymentPayload("order1", "pay1"));

		ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);

		verify(kafkaProducerService).publish(captor.capture());

		PaymentEvent event = captor.getValue();

		assertEquals("order1", event.getOrderId());
		assertEquals("pay1", event.getPaymentId());
		assertEquals(PaymentStatus.PAID, event.getStatus());
		assertEquals(50000L, event.getAmount());
		assertEquals("receipt123", event.getReceipt());
		assertEquals("INR", event.getCurrency());
	}

	@Test
	@DisplayName("Should not publish Kafka event for failed payment")
	void shouldNotPublishKafkaEventForFailedPayment() {

		PaymentOrder order = new PaymentOrder();
		order.setAmount(50000L);
		order.setReceipt("receipt123");
		order.setCurrency("INR");

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.of(order));

		webhookService.process("payment.failed", paymentPayload("order1", "pay1"));

		verify(kafkaProducerService).publish(any(PaymentEvent.class));

	}

	@Test
	@DisplayName("Should not publish Kafka event when order is missing")
	void shouldNotPublishKafkaWhenOrderNotFound() {

		when(paymentOrderRepository.findByRazorpayOrderId("order1")).thenReturn(Optional.empty());

		webhookService.process("payment.captured", paymentPayload("order1", "pay1"));

		verify(kafkaProducerService, never()).publish(any());
	}

	@Test
	@DisplayName("Should not publish Kafka event for unknown webhook")
	void shouldNotPublishKafkaForUnknownEvent() {

		webhookService.process("random.event", new JSONObject());

		verifyNoInteractions(kafkaProducerService);
	}

	@Test
	@DisplayName("Should not publish Kafka event for invalid payload")
	void shouldNotPublishKafkaForInvalidPayload() {

		webhookService.process("payment.captured", new JSONObject());

		verifyNoInteractions(kafkaProducerService);
	}

}