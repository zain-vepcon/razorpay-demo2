package com.example.razorpay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Configuration
public class RazorpayConfig {

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	/**
	 * RazorpayClient is the official SDK client used to talk to the Razorpay
	 * Orders/Payments API. Get test-mode keys from: Razorpay Dashboard -> Settings
	 * -> API Keys (make sure "Test Mode" toggle is ON).
	 */
	@Bean
	RazorpayClient razorpayClient() throws RazorpayException {
		return new RazorpayClient(keyId, keySecret);
	}
}
