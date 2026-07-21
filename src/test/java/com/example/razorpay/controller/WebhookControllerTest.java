package com.example.razorpay.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.razorpay.security.WebhookSignatureValidator;
import com.example.razorpay.service.AsyncWebhookService;
import com.example.razorpay.service.WebhookService;

/**
 * Unit tests for {@link WebhookController}.
 *
 * <p>
 * Tests Razorpay webhook processing scenarios:
 * </p>
 *
 * <ul>
 * <li>Missing signature</li>
 * <li>Invalid signature</li>
 * <li>Duplicate events</li>
 * <li>Successful webhook processing</li>
 * </ul>
 *
 * @author Zain
 * @since 1.0
 */
@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private WebhookService webhookService;

	@MockBean
	private WebhookSignatureValidator signatureValidator;

	@MockBean
	private AsyncWebhookService asyncWebhookService;

	private final String payload = """
			{
			  "event":"payment.captured",
			  "payload":{
			    "payment":{
			      "entity":{
			        "id":"pay_123",
			        "order_id":"order_123"
			      }
			    }
			  }
			}
			""";

	private final String signature = "signature123";

	private final String eventId = "evt_123";

	@BeforeEach
	void setup() throws Exception {

		// Inject @Value field
//		WebhookController controller = new WebhookController(webhookService);

	}

	/**
	 * Missing signature header should reject request.
	 */
	@Test
	void handleWebhook_missingSignature() throws Exception {

		mockMvc.perform(post("/api/webhooks/razorpay").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest()).andExpect(content().string("Missing signature header"));

		verifyNoInteractions(webhookService);
		verifyNoInteractions(asyncWebhookService);
		verifyNoInteractions(signatureValidator);

	}

	/**
	 * Invalid signature should record event but should not process it.
	 */
	@Test
	void handleWebhook_invalidSignature() throws Exception {

		when(signatureValidator.isValid(anyString(), anyString(), anyString())).thenReturn(false);

		mockMvc.perform(post("/api/webhooks/razorpay").header("X-Razorpay-Signature", signature)
				.header("X-Razorpay-Event-Id", eventId).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest()).andExpect(content().string("Invalid signature"));

		verify(webhookService).recordEvent(eq(eventId), eq("payment.captured"), eq(payload), eq(false),
				any(JSONObject.class));

		verify(asyncWebhookService, never()).processAsync(anyString(), any());

	}

	/**
	 * Duplicate events should not be processed again.
	 */
	@Test
	void handleWebhook_duplicateEvent() throws Exception {

		when(signatureValidator.isValid(anyString(), anyString(), anyString())).thenReturn(true);

		when(webhookService.isDuplicate(eventId)).thenReturn(true);

		mockMvc.perform(post("/api/webhooks/razorpay").header("X-Razorpay-Signature", signature)
				.header("X-Razorpay-Event-Id", eventId).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk()).andExpect(content().string("Duplicate event acknowledged"));

		verify(asyncWebhookService, never()).processAsync(anyString(), any());

	}

	/**
	 * Valid webhook should be recorded and forwarded for processing.
	 */
	@Test
	void handleWebhook_success() throws Exception {

		when(signatureValidator.isValid(anyString(), anyString(), anyString())).thenReturn(true);

		when(webhookService.isDuplicate(eventId)).thenReturn(false);

		mockMvc.perform(post("/api/webhooks/razorpay").header("X-Razorpay-Signature", signature)
				.header("X-Razorpay-Event-Id", eventId).contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isOk()).andExpect(content().string("Webhook processed"));

		verify(webhookService).recordEvent(eq(eventId), eq("payment.captured"), eq(payload), eq(true),
				any(JSONObject.class));

		verify(asyncWebhookService).processAsync(eq("payment.captured"), any(JSONObject.class));
	}

	/**
	 * Payload without event field should use unknown event.
	 */
	@Test
	void handleWebhook_unknownEvent() throws Exception {

		String unknownPayload = """
				{
				  "payload":{}
				}
				""";

		when(signatureValidator.isValid(anyString(), anyString(), anyString())).thenReturn(true);

		when(webhookService.isDuplicate(eventId)).thenReturn(false);

		mockMvc.perform(post("/api/webhooks/razorpay").header("X-Razorpay-Signature", signature)
				.header("X-Razorpay-Event-Id", eventId).contentType(MediaType.APPLICATION_JSON).content(unknownPayload))
				.andExpect(status().isOk());

		verify(asyncWebhookService).processAsync(eq("unknown"), any(JSONObject.class));

	}

}