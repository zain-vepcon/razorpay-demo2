package com.example.razorpay.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.example.razorpay.util.SignatureUtil;

class WebhookSignatureValidatorTest {

	private final WebhookSignatureValidator validator = new WebhookSignatureValidator();

	@Test
	void isValid_shouldReturnTrue() {

		try (MockedStatic<SignatureUtil> mocked = org.mockito.Mockito.mockStatic(SignatureUtil.class)) {

			mocked.when(() -> SignatureUtil.verifySignature("payload", "signature", "secret")).thenReturn(true);

			assertTrue(validator.isValid("payload", "signature", "secret"));
		}
	}

	@Test
	void isValid_shouldReturnFalse() {

		try (MockedStatic<SignatureUtil> mocked = org.mockito.Mockito.mockStatic(SignatureUtil.class)) {

			mocked.when(() -> SignatureUtil.verifySignature("payload", "signature", "secret")).thenReturn(false);

			assertFalse(validator.isValid("payload", "signature", "secret"));
		}
	}

}