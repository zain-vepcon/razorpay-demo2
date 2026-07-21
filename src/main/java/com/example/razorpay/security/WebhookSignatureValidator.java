package com.example.razorpay.security;

import org.springframework.stereotype.Component;

import com.example.razorpay.util.SignatureUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates Razorpay webhook signatures.
 *
 * <p>
 * This component isolates signature validation logic from controllers and makes
 * webhook processing easier to test.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Slf4j
@Component
public class WebhookSignatureValidator {

	/**
	 * Validates webhook payload signature.
	 *
	 * @param payload   raw Razorpay webhook payload
	 * @param signature Razorpay signature header value
	 * @param secret    webhook secret configured in application properties
	 *
	 * @return true if signature is valid, otherwise false
	 */
	public boolean isValid(String payload, String signature, String secret) {

		return SignatureUtil.verifySignature(payload, signature, secret);
	}

}