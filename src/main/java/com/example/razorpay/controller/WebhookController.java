package com.example.razorpay.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.razorpay.service.WebhookService;
import com.example.razorpay.util.SignatureUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for handling Razorpay webhook events.
 *
 * <p>
 * Receives webhook notifications from Razorpay, validates the webhook
 * signature, prevents duplicate event processing, records incoming events, and
 * delegates business processing to the webhook service.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

	private final WebhookService webhookService;
	private static final String UNKNOWN_EVENT = "unknown";

	@Value("${razorpay.webhook.secret}")
	private String webhookSecret;

	/**
	 * Processes incoming Razorpay webhook events.
	 *
	 * <p>
	 * The request signature is verified using the configured webhook secret before
	 * processing the payload. Duplicate events are ignored using the Razorpay event
	 * ID to ensure idempotent processing.
	 * </p>
	 *
	 * @param rawPayload raw webhook payload
	 * @param signature  Razorpay webhook signature
	 * @param eventId    unique Razorpay event identifier
	 * @return webhook processing status
	 */
	@PostMapping("/razorpay")
	public ResponseEntity<String> handleWebhook(@RequestBody String rawPayload,
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
			@RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {

		if (signature == null || signature.isBlank()) {
			log.warn("Webhook rejected: missing X-Razorpay-Signature header");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
		}
		log.info("Webhook Secret = {}", webhookSecret);

		boolean signatureValid = SignatureUtil.verifySignature(rawPayload, signature, webhookSecret);

		JSONObject json = new JSONObject(rawPayload);
		String eventType = json.optString("event", UNKNOWN_EVENT);

		if (!signatureValid) {
			log.warn("Webhook signature validation failed. eventType={}, eventId={}", eventType, eventId);
			webhookService.recordEvent(eventId, eventType, rawPayload, false);
			// 400 tells Razorpay something is wrong; do not process the payload as trusted.
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
		}

		if (webhookService.isDuplicate(eventId)) {
			log.info("Duplicate webhook ignored: eventId={} type={}", eventId, eventType);
			// Still return 200 - this is a retry of something we already processed
			// successfully.
			return ResponseEntity.ok("Duplicate event acknowledged");
		}

		webhookService.recordEvent(eventId, eventType, rawPayload, true);
		webhookService.process(eventType, json);

		return ResponseEntity.ok("Webhook processed");
	}
}
