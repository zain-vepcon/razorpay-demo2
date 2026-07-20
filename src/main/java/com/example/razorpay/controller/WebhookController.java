package com.example.razorpay.controller;

import com.example.razorpay.service.WebhookService;
import com.example.razorpay.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    /**
     * Razorpay webhook endpoint. Configure this URL in:
     * Dashboard -> Settings -> Webhooks -> Add New Webhook
     * e.g. https://<your-ngrok-domain>/api/webhooks/razorpay
     *
     * IMPORTANT:
     * 1. The raw request body (untouched, exact bytes as sent) must be used to compute the
     *    signature - NOT a re-serialized version of the parsed JSON. Spring binds it here as
     *    a String specifically to preserve the exact bytes.
     * 2. Always return 2xx quickly. If you return non-2xx or time out, Razorpay will retry
     *    the webhook (with backoff) - hence the idempotency check using x-razorpay-event-id.
     * 3. Do any slow work (emails, external calls) asynchronously after acknowledging receipt.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {

        if (signature == null) {
            log.warn("Webhook rejected: missing X-Razorpay-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
        }
        log.info("Webhook Secret = {}", webhookSecret);

        boolean signatureValid = SignatureUtil.verifySignature(rawPayload, signature, webhookSecret);

        JSONObject json = new JSONObject(rawPayload);
        String eventType = json.optString("event", "unknown");

        if (!signatureValid) {
            log.warn("Webhook signature verification FAILED for event={} eventId={}", eventType, eventId);
            webhookService.recordEvent(eventId, eventType, rawPayload, false);
            // 400 tells Razorpay something is wrong; do not process the payload as trusted.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if (webhookService.isDuplicate(eventId)) {
            log.info("Duplicate webhook ignored: eventId={} type={}", eventId, eventType);
            // Still return 200 - this is a retry of something we already processed successfully.
            return ResponseEntity.ok("Duplicate event acknowledged");
        }

        webhookService.recordEvent(eventId, eventType, rawPayload, true);
        webhookService.process(eventType, json);

        return ResponseEntity.ok("Webhook processed");
    }
}
