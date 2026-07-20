package com.example.razorpay.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Razorpay signs both (a) the checkout success payload and (b) every webhook
 * body using HMAC-SHA256. Verifying this signature is what proves the request
 * actually came from Razorpay and was not forged or tampered with in transit.
 */
public final class SignatureUtil {

	private SignatureUtil() {
	}

	/**
	 * Computes HMAC-SHA256(payload, secret) and returns it as a lowercase hex
	 * string, then compares it to the signature Razorpay sent using a constant-time
	 * comparison (to avoid timing attacks).
	 */
	public static boolean verifySignature(String payload, String signature, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			String computedSignature = HexFormat.of().formatHex(hash);
			return MessageDigest.isEqual(computedSignature.getBytes(StandardCharsets.UTF_8),
					signature.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}
}
