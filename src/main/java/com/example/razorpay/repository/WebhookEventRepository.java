package com.example.razorpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.razorpay.model.WebhookEvent;

/**
 * Repository for managing {@link WebhookEvent} entities.
 *
 * <p>
 * Supports persistence and lookup of webhook events for duplicate detection,
 * auditing, and troubleshooting.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
//@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

	/**
	 * Retrieves a webhook event using its unique Razorpay event ID.
	 *
	 * @param eventId Razorpay webhook event identifier
	 * @return matching webhook event if found
	 */
	Optional<WebhookEvent> findByEventId(String eventId);

}