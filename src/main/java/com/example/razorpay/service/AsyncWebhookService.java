package com.example.razorpay.service;

import org.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Executes webhook processing asynchronously.
 *
 * <p>
 * Delegates actual business processing to {@link WebhookService} while running
 * on a dedicated executor thread.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class AsyncWebhookService {

	private final WebhookService webhookService;

	/**
	 * Processes webhook asynchronously.
	 *
	 * @param eventType webhook event type
	 * @param payload   webhook payload
	 */
	@Async("webhookExecutor")
	public void processAsync(String eventType, JSONObject payload) {

		webhookService.process(eventType, payload);
	}

}