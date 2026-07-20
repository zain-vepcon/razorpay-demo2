package com.example.razorpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response returned after successfully creating a Razorpay order.
 *
 * <p>
 * Contains all details required by the frontend to initialize Razorpay
 * Checkout.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public class OrderResponse {

	/**
	 * Razorpay-generated order identifier.
	 */
	private final String razorpayOrderId;

	/**
	 * Payment amount in paise.
	 */
	private final Long amount;

	/**
	 * Payment currency.
	 */
	private final String currency;

	/**
	 * Merchant receipt identifier.
	 */
	private final String receipt;

	/**
	 * Razorpay order status.
	 */
	private final String status;

	/**
	 * Razorpay API key used by Checkout.js.
	 */
	private final String keyId;
}