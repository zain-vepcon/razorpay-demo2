package com.example.razorpay.service;

import com.example.razorpay.model.PaymentOrder;

/**
 * Contract for post-payment business operations.
 *
 * <p>
 * Provides fulfillment workflows for both successful and failed payment
 * scenarios.
 * </p>
 */
public interface FulfillmentService {

	/**
	 * Executes post-payment fulfillment after a successful payment.
	 *
	 * <p>
	 * Typical operations include:
	 * <ul>
	 * <li>Invoice generation</li>
	 * <li>Confirmation email</li>
	 * <li>SMS notification</li>
	 * <li>Subscription activation</li>
	 * </ul>
	 *
	 * @param order successfully paid order
	 */
	void fulfillOrder(PaymentOrder order);

	/**
	 * Executes post-payment activities after a failed payment.
	 *
	 * <p>
	 * Typical operations include:
	 * <ul>
	 * <li>Failure email notification</li>
	 * <li>Failure SMS notification</li>
	 * <li>Audit logging</li>
	 * </ul>
	 *
	 * @param order failed payment order
	 */
	void handlePaymentFailure(PaymentOrder order);

}
