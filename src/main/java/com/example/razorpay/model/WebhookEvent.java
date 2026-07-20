package com.example.razorpay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Razorpay's x-razorpay-event-id header - used to de-duplicate retried webhooks */
    @Column(unique = true)
    private String eventId;

    /** e.g. payment.captured, payment.failed, order.paid, refund.processed */
    private String eventType;

    @Lob
    private String rawPayload;

    private boolean signatureValid;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    @PrePersist
    void onCreate() {
        receivedAt = Instant.now();
    }
}
