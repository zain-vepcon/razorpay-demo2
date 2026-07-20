package com.example.razorpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    /** Amount in RUPEES (not paise) - the service converts it for you */
    @NotNull
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    private Double amount;

    @NotBlank
    private String currency = "INR";

    @NotBlank
    private String receipt;
}
