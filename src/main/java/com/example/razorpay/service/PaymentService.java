package com.example.razorpay.service;

import java.util.List;

import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.OrderResponse;
import com.example.razorpay.dto.PaymentFailureRequest;
import com.example.razorpay.dto.PaymentVerificationRequest;
import com.example.razorpay.model.PaymentOrder;
import com.razorpay.RazorpayException;

/**
 * Service interface for payment operations.
 *
 * @author Zain
 * @since 1.0
 */
public interface PaymentService {

	/**
	 * Creates Razorpay order.
	 *
	 * @param request order request
	 * @return order response
	 */
	OrderResponse createOrder(OrderRequest request) throws RazorpayException;

	/**
	 * Verifies Razorpay signature.
	 *
	 * @param request payment verification request
	 * @return true if valid
	 */
	boolean verifyPaymentSignature(PaymentVerificationRequest request);

	/**
	 * Fetch all payments.
	 *
	 * @return payments
	 */
	List<PaymentOrder> getAllPayments();

	/**
	 * Fetch payment by database id.
	 *
	 * @param id payment id
	 * @return payment
	 */
	PaymentOrder getPaymentById(Long id);

	/**
	 * Fetch payment by Razorpay payment id.
	 *
	 * @param paymentId Razorpay payment id
	 * @return payment
	 */
	PaymentOrder getPaymentByPaymentId(String paymentId);

	/**
	 * Fetch payment by Razorpay order id.
	 *
	 * @param orderId Razorpay order id
	 * @return payment
	 */
	PaymentOrder getPaymentByOrderId(String orderId);

	/**
	 * Updates failed payment.
	 *
	 * @param request failure details
	 */
	void updateFailedPayment(PaymentFailureRequest request);

}