package com.example.razorpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request object used to verify the Razorpay payment signature.
 *
 * <p>
 * The frontend submits these values immediately after a successful Razorpay
 * Checkout. The server verifies the signature to confirm the integrity of the
 * payment response.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentVerificationRequest {

	/**
	 * Razorpay order identifier.
	 */
	@NotBlank(message = "Razorpay order ID is required")
	private String razorpayOrderId;

	/**
	 * Razorpay payment identifier.
	 */
	@NotBlank(message = "Razorpay payment ID is required")
	private String razorpayPaymentId;

	/**
	 * Razorpay signature used for verification.
	 */
	@NotBlank(message = "Razorpay signature is required")
	private String razorpaySignature;
}