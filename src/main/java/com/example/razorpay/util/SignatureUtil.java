package com.example.razorpay.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for verifying Razorpay HMAC-SHA256 signatures.
 *
 * <p>
 * Razorpay signs checkout responses and webhook payloads using HMAC-SHA256.
 * This utility validates those signatures to ensure that incoming requests
 * originated from Razorpay and have not been modified during transmission.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
public final class SignatureUtil {

	/**
	 * HMAC SHA-256 algorithm.
	 */
	private static final String HMAC_SHA256 = "HmacSHA256";

	/**
	 * Prevent instantiation.
	 */
	private SignatureUtil() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated.");
	}

	/**
	 * Verifies the Razorpay signature using HMAC-SHA256.
	 *
	 * <p>
	 * A constant-time comparison is performed to minimize timing attack
	 * vulnerabilities.
	 * </p>
	 *
	 * @param payload   payload used for signature generation
	 * @param signature Razorpay signature
	 * @param secret    API secret or webhook secret
	 * @return {@code true} if the signature is valid; {@code false} otherwise
	 */
	public static boolean verifySignature(String payload, String signature, String secret) {

		try {

			Mac mac = Mac.getInstance(HMAC_SHA256);

			SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);

			mac.init(secretKey);

			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

			String computedSignature = HexFormat.of().formatHex(hash);

			return MessageDigest.isEqual(computedSignature.getBytes(StandardCharsets.UTF_8),
					signature.getBytes(StandardCharsets.UTF_8));

		} catch (Exception exception) {

			return false;
		}
	}

}