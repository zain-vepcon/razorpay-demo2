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
import lombok.Setter;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
public class PaymentOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Razorpay order id, e.g. order_XXXXXXXXXXXXX */
	@Column(nullable = false, unique = true)
	private String razorpayOrderId;

	/** Razorpay payment id once a payment is attempted, e.g. pay_XXXXXXXXXXXXX */
	private String razorpayPaymentId;

	/** Our internal receipt / reference number sent when creating the order */
	private String receipt;

	/** Amount in smallest currency unit (paise for INR) */
	private Long amount;

	private String currency;

	/** CREATED, ATTEMPTED, PAID, FAILED, REFUNDED */
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	private String failureCode;

	private String failureReason;

	private String failureSource;

	private String failureStep;

	private String failureReasonCode;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = Instant.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
