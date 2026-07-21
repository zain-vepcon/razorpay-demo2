package com.example.razorpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request object used to create a Razorpay order.
 *
 * <p>
 * The amount is provided in Indian Rupees (INR). The service layer converts it
 * into paise before sending the request to Razorpay.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

	/**
	 * Payment amount in Indian Rupees.
	 */
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "1.0", message = "Amount must be at least 1")
	private Double amount;

	/**
	 * Payment currency.
	 */
	@NotBlank(message = "Currency is required")
	private String currency = "INR";

	/**
	 * Merchant receipt identifier.
	 */
	@NotBlank(message = "Receipt is required")
	private String receipt;

	/**
	 * customer Name
	 */
	@NotBlank(message = "Name is required")
	private String customerName;

	/**
	 * customer Email
	 */
	@Email
	@NotBlank(message = "Email is required")
	private String customerEmail;

	@NotBlank(message = "Phone Number is required")
	private String customerPhone;
}
