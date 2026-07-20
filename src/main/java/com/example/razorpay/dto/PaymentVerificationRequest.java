package com.example.razorpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Sent by the frontend after Razorpay Checkout succeeds, containing the three
 * fields returned in the handler callback: razorpay_order_id, razorpay_payment_id,
 * razorpay_signature. This is used for immediate client-side confirmation;
 * the webhook remains the source of truth for server-side reconciliation.
 */
@Getter
@Setter
public class PaymentVerificationRequest {

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;
}
