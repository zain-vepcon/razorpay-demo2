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

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	/**
	 * Step 1 of the payment flow: create a Razorpay order before showing
	 * Checkout.js on the frontend. Test with: POST /api/payments/create-order
	 */
	@PostMapping("/create-order")
	public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
		try {
			OrderResponse response = paymentService.createOrder(request);
			return ResponseEntity.ok(ApiResponse.ok("Order created", response));
		} catch (RazorpayException e) {
			log.error("Failed to create Razorpay order", e);
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body(ApiResponse.fail("Could not create order: " + e.getMessage()));
		}
	}

	/**
	 * Step 2 (optional, client-side confirmation): frontend calls this right after
	 * Checkout.js's success handler fires, passing back razorpay_order_id,
	 * razorpay_payment_id, razorpay_signature. This gives instant UI feedback, but
	 * final fulfillment should always wait on/reconcile with the webhook.
	 */
	@PostMapping("/verify")
	public ResponseEntity<ApiResponse<Boolean>> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {

		boolean valid = paymentService.verifyPaymentSignature(request);

		if (valid) {

			return ResponseEntity.ok(ApiResponse.ok("Signature verified", true));

		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Signature verification failed"));

	}

//	@PostMapping("/failed")
//	public ResponseEntity<ApiResponse<String>> paymentFailed(@RequestBody PaymentFailureRequest request) {
//
//		log.info("Payment Failed : {}", request);
//
//		paymentService.updateFailedPayment(request);
//
//		return ResponseEntity.ok(ApiResponse.ok("Failure recorded", "SUCCESS"));
//	}

	/**
	 * Get all payments.
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<PaymentOrder>>> getAllPayments() {

		List<PaymentOrder> payments = paymentService.getAllPayments();

		return ResponseEntity.ok(ApiResponse.ok("Payments fetched successfully", payments));
	}

	/**
	 * Get payment by database ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getPaymentById(@PathVariable Long id) {

		PaymentOrder payment = paymentService.getPaymentById(id);

		return ResponseEntity.ok(ApiResponse.ok("Payment fetched successfully", payment));
	}

	/**
	 * Get payment using Razorpay Payment ID.
	 */
	@GetMapping("/payment-id/{paymentId}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getByPaymentId(@PathVariable String paymentId) {

		PaymentOrder payment = paymentService.getPaymentByPaymentId(paymentId);

		return ResponseEntity.ok(ApiResponse.ok("Payment fetched successfully", payment));
	}

	/**
	 * Get payment using Razorpay Order ID.
	 */
	@GetMapping("/order/{orderId}")
	public ResponseEntity<ApiResponse<PaymentOrder>> getByOrderId(@PathVariable String orderId) {

		PaymentOrder payment = paymentService.getPaymentByOrderId(orderId);

		return ResponseEntity.ok(ApiResponse.ok("Payment fetched successfully", payment));
	}
}
