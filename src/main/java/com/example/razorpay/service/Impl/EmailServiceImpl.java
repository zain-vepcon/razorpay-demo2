package com.example.razorpay.service.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link EmailService}.
 *
 * <p>
 * Responsible for sending payment-related email notifications. Email delivery
 * is performed asynchronously so that the payment processing thread is not
 * blocked.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	/**
	 * Sends a payment success email.
	 *
	 * @param recipient    customer email
	 * @param customerName customer name
	 * @param orderId      Razorpay order id
	 * @param paymentId    Razorpay payment id
	 * @param amount       payment amount in paise
	 */
	@Override
	@Async
	public void sendPaymentSuccessEmail(String recipient, String customerName, String orderId, String paymentId,
			Long amount) {

		try {

			SimpleMailMessage mail = new SimpleMailMessage();

			mail.setFrom(fromEmail);

			mail.setTo(recipient);

			mail.setSubject("Payment Successful");

			mail.setText(createEmailBody(customerName, orderId, paymentId, amount));

			mailSender.send(mail);

			log.info("Payment confirmation email sent successfully to {} for OrderId={}", recipient, orderId);

		} catch (Exception ex) {

			log.error("Failed to send payment confirmation email for OrderId={}", orderId, ex);
		}
	}

	/**
	 * Convenience method for sending an email using a PaymentOrder.
	 *
	 * @param order payment order
	 */
	@Override
	public void sendPaymentSuccessEmail(PaymentOrder order) {

		sendPaymentSuccessEmail("mohammed.zain@vepconsoftsystems.com", // TODO -> order.getCustomerEmail()
				"ZAIN", // TODO -> order.getCustomerName()
				order.getRazorpayOrderId(), order.getRazorpayPaymentId(), order.getAmount());
	}

	/**
	 * Builds the email body.
	 *
	 * @param customerName customer name
	 * @param orderId      Razorpay order id
	 * @param paymentId    Razorpay payment id
	 * @param amount       payment amount in paise
	 * @return formatted email body
	 */
	private String createEmailBody(String customerName, String orderId, String paymentId, Long amount) {

		return """
				Dear %s,

				Your payment has been received successfully.

				Order ID   : %s
				Payment ID : %s
				Amount     : ₹%.2f

				Thank you for choosing us.

				Regards,
				Razorpay Payment Team
				""".formatted(customerName, orderId, paymentId, amount / 100.0);
	}

	/**
	 *
	 */
	@Override
	@Async
	public void sendPaymentFailureEmail(String recipient, String customerName, String orderId, String paymentId,
			String reason) {

		try {

			SimpleMailMessage mail = new SimpleMailMessage();

			mail.setFrom(fromEmail);

			mail.setTo(recipient);

			mail.setSubject("Payment Failed");

			mail.setText(createFailureEmailBody(customerName, orderId, paymentId, reason));

			mailSender.send(mail);

			log.info("Payment failure email sent to {}", recipient);

		} catch (Exception ex) {

			log.error("Unable to send payment failure email", ex);
		}
	}

	/**
	 *
	 */
	@Override
	public void sendPaymentFailureEmail(PaymentOrder order) {

		sendPaymentFailureEmail(

				"mohammed.zain@vepconsoftsystems.com", // TODO order.getCustomerEmail()

				"ZAIN", // TODO order.getCustomerName()

				order.getRazorpayOrderId(),

				order.getRazorpayPaymentId(),

				order.getFailureReason()

		);
	}

	/**
	 * Creates payment failure email body.
	 *
	 * @param customerName customer name
	 * @param orderId      Razorpay order id
	 * @param paymentId    Razorpay payment id
	 * @param reason       payment failure reason
	 *
	 * @return formatted email body
	 */
	private String createFailureEmailBody(String customerName, String orderId, String paymentId, String reason) {

		return """
				Dear %s,

				Unfortunately, your payment could not be completed.

				Order ID   : %s
				Payment ID : %s

				Reason:

				%s

				Please try again using another payment method.

				Regards,
				Razorpay Payment Team
				""".formatted(customerName, orderId, paymentId, reason);
	}
}