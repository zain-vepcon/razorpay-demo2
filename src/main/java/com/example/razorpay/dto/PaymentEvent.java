package com.example.razorpay.dto;

import com.example.razorpay.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentEvent {

	private String eventType;

	private String paymentId;

	private String orderId;

	private Long amount;

	private String receipt;

	private PaymentStatus status;

	private String currency;

	private String customerEmail;

	private String customerPhone;

	private String failureReason;

	private Long timestamp;

}
