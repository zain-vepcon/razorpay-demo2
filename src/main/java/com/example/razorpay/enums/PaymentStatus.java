package com.example.razorpay.enums;

/**
 * Represents the lifecycle states of a payment.
 *
 * <p>
 * A payment progresses through these states based on the events received from
 * Razorpay.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
public enum PaymentStatus {

	/**
	 * Order has been created successfully.
	 */
	CREATED,

	/**
	 * Customer has initiated the payment process.
	 */
	ATTEMPTED,

	/**
	 * Payment has been authorized by the issuing bank.
	 */
	AUTHORIZED,

	/**
	 * Payment has been captured successfully.
	 */
	PAID,

	/**
	 * Payment attempt failed.
	 */
	FAILED,

	/**
	 * Payment has been refunded.
	 */
	REFUNDED
}
