package com.example.razorpay.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.Impl.EmailServiceImpl;

/**
 * Unit tests for {@link EmailServiceImpl}.
 *
 * <p>
 * Validates email generation and email sending behaviour. External SMTP
 * communication is mocked using Mockito.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private EmailServiceImpl emailService;

	private PaymentOrder paymentOrder;

	@BeforeEach
	void setup() throws Exception {

		paymentOrder = new PaymentOrder();

		paymentOrder.setRazorpayOrderId("order_123");

		paymentOrder.setRazorpayPaymentId("pay_123");

		paymentOrder.setAmount(50000L);

		paymentOrder.setStatus(PaymentStatus.PAID);

		/*
		 * Inject spring.mail.username
		 */
		Field field = EmailServiceImpl.class.getDeclaredField("fromEmail");

		field.setAccessible(true);

		field.set(emailService, "demo@gmail.com");
	}

	/**
	 * Verifies successful email sending.
	 */
	@Test
	@DisplayName("Should send payment confirmation email successfully")
	void sendPaymentSuccessEmail_shouldSendEmail() {

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));

		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	/**
	 * Verifies that exceptions from JavaMailSender are handled gracefully.
	 */
	@Test
	@DisplayName("Should handle mail sender exception")
	void sendPaymentSuccessEmail_shouldHandleException() {

		doThrow(new MailException("SMTP Error") {
			private static final long serialVersionUID = 1L;
		}).when(mailSender).send(any(SimpleMailMessage.class));

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));
	}

	/**
	 * Verifies null payment id does not crash service.
	 */
	@Test
	@DisplayName("Should handle null payment id")
	void sendPaymentSuccessEmail_nullPaymentId() {

		paymentOrder.setRazorpayPaymentId(null);

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));
	}

	/**
	 * Verifies null amount.
	 */
	@Test
	@DisplayName("Should handle null amount")
	void sendPaymentSuccessEmail_nullAmount() {

		paymentOrder.setAmount(null);

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));
	}

	/**
	 * Verifies null order id.
	 */
	@Test
	@DisplayName("Should handle null order id")
	void sendPaymentSuccessEmail_nullOrderId() {

		paymentOrder.setRazorpayOrderId(null);

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));
	}

	/**
	 * Verifies null status.
	 */
	@Test
	@DisplayName("Should handle null payment status")
	void sendPaymentSuccessEmail_nullStatus() {

		paymentOrder.setStatus(null);

		assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(paymentOrder));
	}

	@Test
	@DisplayName("Should send payment failure email")
	void shouldSendPaymentFailureEmail() {

		emailService.sendPaymentFailureEmail("test@test.com", "Zain", "order123", "pay123", "Bank declined");

		verify(mailSender).send(any(SimpleMailMessage.class));
	}

}
