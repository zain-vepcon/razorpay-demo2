package com.example.razorpay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrderResponse {
    private String razorpayOrderId;
    private Long amount;
    private String currency;
    private String receipt;
    private String status;
    /** Sent to frontend so Razorpay Checkout.js can open the payment widget */
    private String keyId;
}
