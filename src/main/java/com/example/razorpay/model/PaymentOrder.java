package com.example.razorpay.model;

import java.time.Instant;

import com.example.razorpay.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a payment order.
 *
 * <p>
 * Stores payment details created through Razorpay and tracks the payment
 * lifecycle from order creation to refund.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Razorpay order identifier. */
	@Column(nullable = false, unique = true)
	private String razorpayOrderId;

	/** Razorpay payment identifier. */
	private String razorpayPaymentId;

	/** Merchant receipt identifier. */
	private String receipt;

	/** Payment amount in the smallest currency unit (paise). */
	private Long amount;

	/** Payment currency. */
	private String currency;

	/** Current payment status. */
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	/** Failure code returned by Razorpay. */
	private String failureCode;

	/** Failure description. */
	private String failureReason;

	/** Failure source. */
	private String failureSource;

	/** Processing step where the failure occurred. */
	private String failureStep;

	/** Failure reason code. */
	private String failureReasonCode;

	/** Record creation timestamp. */
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	/** Record last update timestamp. */
	private Instant updatedAt;

	/**
	 * Initializes timestamps before entity persistence.
	 */
	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	/**
	 * Updates the modification timestamp before entity update.
	 */
	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}