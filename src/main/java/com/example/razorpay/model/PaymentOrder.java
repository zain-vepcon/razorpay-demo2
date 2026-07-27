package com.example.razorpay.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
public class PaymentOrder implements Serializable {
	private static final long serialVersionUID = 1L;

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

	/** Failure description. */
	private String failureReason;

	/** Record creation timestamp. */
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** Record last update timestamp. */

	private LocalDateTime updatedAt;

	@Column(nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String customerPhone;

	@Column(name = "payment_created_at")
	private LocalDateTime paymentCreatedAt;

	/**
	 * Initializes timestamps before entity persistence.
	 */
	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		createdAt = now;
		updatedAt = now;
	}

	/**
	 * Updates the modification timestamp before entity update.
	 */
	@PreUpdate
	public void onUpdate() {
		updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
	}

}