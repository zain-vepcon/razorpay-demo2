package com.example.razorpay.service;

import org.json.JSONObject;

/**
 * Contract for handling Razorpay webhook events.
 *
 * <p>
 * Defines the operations required for webhook processing:
 * <ul>
 * <li>Deduplication of events</li>
 * <li>Recording raw webhook payloads</li>
 * <li>Routing events to handlers</li>
 * <li>Updating payment order status</li>
 * <li>Triggering fulfillment workflows</li>
 * </ul>
 * </p>
 */
public interface WebhookService {

	boolean isDuplicate(String eventId);

	void recordEvent(String eventId, String eventType, String rawPayload, boolean signatureValid, JSONObject payload);

	void process(String eventType, JSONObject payload);
}
