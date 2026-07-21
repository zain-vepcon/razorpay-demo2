package com.example.razorpay.model;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a Razorpay webhook event.
 *
 * <p>
 * Stores incoming webhook payloads for auditing, signature validation,
 * duplicate detection, and troubleshooting.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Unique Razorpay webhook event identifier. */
	@Column(unique = true)
	private String eventId;

	/** Razorpay webhook event type. */
	private String eventType;

	/** Original webhook payload. */
	@Lob
	private String rawPayload;

	/** Indicates whether the webhook signature was successfully validated. */
	private boolean signatureValid;

	/** Timestamp when the webhook was received. */
	@Column(nullable = false, updatable = false)
	private Instant receivedAt;
	
	@Column(name = "payment_created_at")
	private LocalDateTime CreatedAt;

	/**
	 * Sets the received timestamp before persisting the entity.
	 */
	@PrePersist
	protected void onCreate() {
		receivedAt = Instant.now();
	}
}