package com.example.razorpay.service;

import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.OrderResponse;
import com.example.razorpay.dto.PaymentFailureRequest;
import com.example.razorpay.dto.PaymentVerificationRequest;
import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.util.SignatureUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final RazorpayClient razorpayClient;
	private final PaymentOrderRepository paymentOrderRepository;

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	/**
	 * Creates an order on Razorpay's side. This MUST happen before Checkout.js is
	 * opened on the frontend - Razorpay requires a valid order_id to initiate
	 * payment. Amount must be converted to the smallest currency unit (paise for
	 * INR): 1 INR = 100 paise.
	 */
	public OrderResponse createOrder(OrderRequest request) throws RazorpayException {
		long amountInPaise = Math.round(request.getAmount() * 100);

		JSONObject orderRequestJson = new JSONObject();
		orderRequestJson.put("amount", amountInPaise);
		orderRequestJson.put("currency", request.getCurrency());
		orderRequestJson.put("receipt", request.getReceipt());
		// "1" auto-captures the payment as soon as authorization succeeds (recommended
		// for most cases).
		// Use "0" if you want to manually capture later (e.g. after fraud checks).
		orderRequestJson.put("payment_capture", 1);

		Order order = razorpayClient.orders.create(orderRequestJson);

		PaymentOrder entity = new PaymentOrder();
		entity.setRazorpayOrderId(order.get("id"));
		entity.setAmount(amountInPaise);
		entity.setCurrency(request.getCurrency());
		entity.setReceipt(request.getReceipt());
		entity.setStatus(PaymentStatus.CREATED);
		paymentOrderRepository.save(entity);

		return new OrderResponse(order.get("id"), amountInPaise, request.getCurrency(), request.getReceipt(), "CREATED",
				keyId);
	}

	/**
	 * Verifies the signature returned by Razorpay Checkout in the browser's success
	 * handler. Formula per Razorpay docs: HMAC_SHA256(order_id + "|" + payment_id,
	 * key_secret) This is a client-side convenience check ONLY. The webhook
	 * (server-to-server) is the authoritative confirmation and must always be
	 * relied on for fulfilling orders.
	 */
	public boolean verifyPaymentSignature(PaymentVerificationRequest request) {

		String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

		return SignatureUtil.verifySignature(payload, request.getRazorpaySignature(), keySecret);
	}

	public List<PaymentOrder> getAllPayments() {
		return paymentOrderRepository.findAll();
	}

	public PaymentOrder getPaymentById(Long id) {
		return paymentOrderRepository.findById(id).orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	public PaymentOrder getPaymentByPaymentId(String paymentId) {
		return paymentOrderRepository.findByRazorpayPaymentId(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	public PaymentOrder getPaymentByOrderId(String orderId) {
		return paymentOrderRepository.findByRazorpayOrderId(orderId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
	}

	public void updateFailedPayment(PaymentFailureRequest request) {

		paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).ifPresent(order -> {

			order.setStatus(PaymentStatus.FAILED);

			order.setRazorpayPaymentId(request.getRazorpayPaymentId());

			order.setFailureCode(request.getErrorCode());

			order.setFailureReason(request.getErrorDescription());

			order.setFailureSource(request.getErrorSource());

			order.setFailureStep(request.getErrorStep());

			paymentOrderRepository.save(order);

		});

	}
}
