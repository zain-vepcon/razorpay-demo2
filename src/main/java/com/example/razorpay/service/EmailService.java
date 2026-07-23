package com.example.razorpay.service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.model.PaymentOrder;

public interface EmailService {

	/**
	 * Sends a payment success email using individual values.
	 */
	void sendPaymentSuccessEmail(String recipient, String customerName, String orderId, String paymentId, Long amount);

	/**
	 * Sends a payment success email using the payment order.
	 */
	void sendPaymentSuccessEmail(PaymentOrder order);

	/**
	 * Sends payment failure email.
	 *
	 * @param order failed payment order
	 */
	void sendPaymentFailureEmail(PaymentOrder order);

	void sendPaymentFailureEmail(String recipient, String customerName, String orderId, String paymentId,
			String reason);

	void sendMail(PaymentEvent event);

}
