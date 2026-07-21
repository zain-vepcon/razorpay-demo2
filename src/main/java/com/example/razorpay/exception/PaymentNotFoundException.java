package com.example.razorpay.exception;

/**
 * Exception thrown when a payment record is not found.
 *
 * @author Zain
 * @since 1.0
 */
public class PaymentNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a payment not found exception.
	 *
	 * @param message exception message
	 */
	public PaymentNotFoundException(String message) {
		super(message);
	}

}