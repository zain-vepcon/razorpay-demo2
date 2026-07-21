package com.example.razorpay.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.EmailService;
import com.example.razorpay.service.Impl.FulfillmentServiceImpl;

/**
 * Unit tests for {@link FulfillmentServiceImpl}.
 *
 * <p>
 * Verifies the post-payment fulfillment workflow after a successful payment.
 * External services are mocked to ensure only business logic is tested.
 * </p>
 *
 * <p>
 * Covered scenarios:
 * <ul>
 * <li>Successful fulfillment</li>
 * <li>Email notification invocation</li>
 * <li>Email service failure</li>
 * <li>Null order validation</li>
 * </ul>
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class FulfillmentServiceImplTest {

	@Mock
	private EmailService emailService;

	@InjectMocks
	private FulfillmentServiceImpl fulfillmentService;

	private PaymentOrder paymentOrder;

	@BeforeEach
	void setup() {

		paymentOrder = new PaymentOrder();

		paymentOrder.setRazorpayOrderId("order_test_123");

		paymentOrder.setRazorpayPaymentId("pay_test_123");

		paymentOrder.setAmount(50000L);

		paymentOrder.setStatus(PaymentStatus.PAID);
	}

	/**
	 * Verifies fulfillment completes successfully.
	 */
	@Test
	@DisplayName("Should complete fulfillment successfully")
	void fulfillOrder_shouldCompleteSuccessfully() {

		assertDoesNotThrow(() -> fulfillmentService.fulfillOrder(paymentOrder));

		verify(emailService).sendPaymentSuccessEmail(paymentOrder);
	}

	/**
	 * Verifies email service invocation.
	 */
	@Test
	@DisplayName("Should invoke EmailService")
	void fulfillOrder_shouldInvokeEmailService() {

		fulfillmentService.fulfillOrder(paymentOrder);

		verify(emailService).sendPaymentSuccessEmail(paymentOrder);
	}

	/**
	 * Verifies email service exception propagation.
	 */
	@Test
	@DisplayName("Should propagate email service exception")
	void fulfillOrder_whenEmailFails_shouldThrowException() {

		doThrow(new RuntimeException("SMTP Error")).when(emailService).sendPaymentSuccessEmail(paymentOrder);

		assertThrows(RuntimeException.class, () -> fulfillmentService.fulfillOrder(paymentOrder));
	}

	/**
	 * Verifies null order handling.
	 */
	@Test
	@DisplayName("Should throw exception when order is null")
	void fulfillOrder_whenOrderIsNull_shouldThrowException() {

		assertThrows(NullPointerException.class, () -> fulfillmentService.fulfillOrder(null));
	}

	/**
	 * Verifies fulfillment works with null payment id.
	 */
	@Test
	@DisplayName("Should handle null payment id")
	void fulfillOrder_nullPaymentId() {

		paymentOrder.setRazorpayPaymentId(null);

		assertDoesNotThrow(() -> fulfillmentService.fulfillOrder(paymentOrder));
	}

	/**
	 * Verifies fulfillment works with null amount.
	 */
	@Test
	@DisplayName("Should handle null amount")
	void fulfillOrder_nullAmount() {

		paymentOrder.setAmount(null);

		assertDoesNotThrow(() -> fulfillmentService.fulfillOrder(paymentOrder));
	}

	/**
	 * Verifies fulfillment works with null status.
	 */
	@Test
	@DisplayName("Should handle null status")
	void fulfillOrder_nullStatus() {

		paymentOrder.setStatus(null);

		assertDoesNotThrow(() -> fulfillmentService.fulfillOrder(paymentOrder));
	}

	/**
	 * Verifies fulfillment works with empty order id.
	 */
	@Test
	@DisplayName("Should handle empty order id")
	void fulfillOrder_emptyOrderId() {

		paymentOrder.setRazorpayOrderId("");

		assertDoesNotThrow(() -> fulfillmentService.fulfillOrder(paymentOrder));
	}

	@Test
	@DisplayName("Should send payment failure email")
	void shouldHandlePaymentFailure() {

		PaymentOrder order = new PaymentOrder();

		fulfillmentService.handlePaymentFailure(order);

		verify(emailService).sendPaymentFailureEmail(order);
	}

}