package com.example.razorpay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

/**
 * Configuration class for Razorpay client initialization.
 *
 * <p>
 * Creates and exposes a singleton {@link RazorpayClient} bean using the API
 * credentials configured in the application properties. The client is used to
 * interact with Razorpay services such as Orders, Payments, Refunds, and
 * Webhooks.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Configuration
public class RazorpayConfig {

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	/**
	 * Creates the Razorpay SDK client.
	 *
	 * <p>
	 * API credentials are injected from the application configuration. Ensure that
	 * valid Test or Live API keys are configured before starting the application.
	 * </p>
	 *
	 * @return configured {@link RazorpayClient}
	 * @throws RazorpayException if the client cannot be initialized
	 */
	@Bean
	RazorpayClient razorpayClient() throws RazorpayException {

		return new RazorpayClient(keyId, keySecret);
	}
}