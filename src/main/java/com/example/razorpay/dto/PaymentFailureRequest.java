package com.example.razorpay.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request object containing Razorpay payment failure details.
 *
 * <p>
 * Captures error information returned by Razorpay when a payment attempt fails.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentFailureRequest {

	/**
	 * Razorpay order identifier.
	 */
	private String razorpayOrderId;

	/**
	 * Razorpay payment identifier.
	 */
	private String razorpayPaymentId;

	private String failureReason;

	/**
	 * Reason for payment failure.
	 */
	private String errorReason;
}