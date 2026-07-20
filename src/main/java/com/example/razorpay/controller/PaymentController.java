package com.example.razorpay.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.razorpay.dto.ApiResponse;
import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.OrderResponse;
import com.example.razorpay.dto.PaymentVerificationRequest;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.PaymentService;
import com.razorpay.RazorpayException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing Razorpay payment operations.
 *
 * <p>
 * Provides APIs to:
 * <ul>
 * <li>Create Razorpay orders</li>
 * <li>Verify payment signatures</li>
 * <li>Retrieve payment details</li>
 * </ul>
 *
 * All responses are wrapped inside {@link ApiResponse}.
 *
 * @author Zain
 * @since 1.0
 */

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	private static final String PAYMENT_FETCHED = "Payment fetched successfully";
	private static final String ORDER_CREATED = "Order created";
	private static final String SIGNATURE_VERIFIED = "Signature verified";
	private static final String SIGNATURE_FAILED = "Signature verification failed";

	/**
	 * Creates a new Razorpay order.
	 *
	 * <p>
	 * The generated order is returned to the frontend and is used to initialize
	 * Razorpay Checkout.
	 * </p>
	 *
	 * @param request payment order request
	 * @return created Razorpay order details
	 */
	@PostMapping("/create-order")
	public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
		try {
			OrderResponse response = paymentService.createOrder(request);
			return ResponseEntity.ok(ApiResponse.ok(ORDER_CREATED, response));
		} catch (RazorpayException e) {
			log.error("Failed to create Razorpay order", e);
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body(ApiResponse.fail("Could not create order: " + e.getMessage()));
		}
	}

	/**
	 * Verifies the Razorpay payment signature.
	 *
	 * <p>
	 * Signature verification confirms that the payment response originated from
	 * Razorpay and was not tampered with.
	 * </p>
	 *
	 * @param request payment verification request
	 * @return verification status
	 */
	@PostMapping("/verify")
	public ResponseEntity<ApiResponse<Boolean>> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {

		boolean valid = paymentService.verifyPaymentSignature(request);

		return valid ? ResponseEntity.ok(ApiResponse.ok(SIGNATURE_VERIFIED, true))
				: ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(SIGNATURE_FAILED));
	}

	/**
	 * Retrieves all payment records.
	 *
	 * @return list of payment records
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<PaymentOrder>>> getAllPayments() {

		List<PaymentOrder> payments = paymentService.getAllPayments();

		return ResponseEntity.ok(ApiResponse.ok(PAYMENT_FETCHED, payments));
	}

	/**
	 * Retrieves a payment by its database identifier.
	 *
	 * @param id payment database ID
	 * @return payment details
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getPaymentById(@PathVariable Long id) {

		PaymentOrder payment = paymentService.getPaymentById(id);

		return ResponseEntity.ok(ApiResponse.ok(PAYMENT_FETCHED, payment));
	}

	/**
	 * Retrieves a payment using its Razorpay payment ID.
	 *
	 * @param paymentId Razorpay payment ID
	 * @return payment details
	 */
	@GetMapping("/payment-id/{paymentId}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getByPaymentId(@PathVariable String paymentId) {

		PaymentOrder payment = paymentService.getPaymentByPaymentId(paymentId);

		return ResponseEntity.ok(ApiResponse.ok(PAYMENT_FETCHED, payment));
	}

	/**
	 * Retrieves a payment using its Razorpay order ID.
	 *
	 * @param orderId Razorpay order ID
	 * @return payment details
	 */
	@GetMapping("/order/{orderId}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getByOrderId(@PathVariable String orderId) {

		PaymentOrder payment = paymentService.getPaymentByOrderId(orderId);

		return ResponseEntity.ok(ApiResponse.ok(PAYMENT_FETCHED, payment));
	}
}
