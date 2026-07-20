package com.example.razorpay.dto;

import lombok.Data;

@Data
public class PaymentFailureRequest {

	private String razorpayOrderId;

	private String razorpayPaymentId;

	private String errorCode;

	private String errorDescription;

	private String errorSource;

	private String errorStep;

	private String errorReason;
}
