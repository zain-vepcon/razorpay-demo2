package com.example.razorpay.service.Impl;

import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.razorpay.client.RazorpayOrderClient;
import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.OrderResponse;
import com.example.razorpay.dto.PaymentFailureRequest;
import com.example.razorpay.dto.PaymentVerificationRequest;
import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.service.EmailService;
import com.example.razorpay.service.PaymentService;
import com.example.razorpay.util.SignatureUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for managing Razorpay payment operations.
 *
 * <p>
 * This service acts as the business layer between REST controllers, Razorpay
 * SDK, and the persistence layer. It provides functionality for:
 * </p>
 * <ul>
 * <li>Creating Razorpay orders</li>
 * <li>Verifying payment signatures</li>
 * <li>Managing payment records</li>
 * <li>Handling payment failures</li>
 * </ul>
 *
 * @author Zain
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	/** Conversion factor from INR to paise. */
	private static final int PAISE_MULTIPLIER = 100;

	/** Razorpay order status returned after successful creation. */
	private static final String CREATED_STATUS = "CREATED";

	/** Error message used when a payment record cannot be found. */
	private static final String PAYMENT_NOT_FOUND = "Payment not found.";

	private final EmailService emailService;

	/**
	 * Wrapper client responsible for Razorpay order operations.
	 */
	private final RazorpayOrderClient razorpayOrderClient;

	private final PaymentOrderRepository paymentOrderRepository;

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${razorpay.key.secret}")
	private String keySecret;

	private static final String AMOUNT = "amount";
	private static final String CURRENCY = "currency";
	private static final String RECEIPT = "receipt";

	/**
	 * Creates a payment order in Razorpay and persists the payment information
	 * locally.
	 *
	 * <p>
	 * The client amount is provided in INR. Razorpay requires the amount in the
	 * smallest currency unit (paise), therefore the amount is multiplied by
	 * {@value #PAISE_MULTIPLIER}.
	 * </p>
	 *
	 * <p>
	 * The workflow is:
	 * </p>
	 *
	 * <ol>
	 * <li>Convert INR amount into paise</li>
	 * <li>Create Razorpay order request</li>
	 * <li>Send request through Razorpay client wrapper</li>
	 * <li>Store payment order details in database</li>
	 * <li>Return order information to frontend</li>
	 * </ol>
	 *
	 * @param request payment order creation request
	 *
	 * @return Razorpay order response containing order id, amount, currency and
	 *         checkout key
	 *
	 * @throws RazorpayException if Razorpay order creation fails
	 */
	public OrderResponse createOrder(OrderRequest request) throws RazorpayException {
		long amountInPaise = Math.round(request.getAmount() * PAISE_MULTIPLIER);

		JSONObject orderRequest = new JSONObject();
		orderRequest.put(AMOUNT, amountInPaise);
		orderRequest.put(CURRENCY, request.getCurrency());
		orderRequest.put(RECEIPT, request.getReceipt());
		orderRequest.put("payment_capture", 1);

		log.info("Creating Razorpay order for receipt={}", request.getReceipt());

		Order order = razorpayOrderClient.createOrder(orderRequest);

		PaymentOrder paymentOrder = new PaymentOrder();
		paymentOrder.setRazorpayOrderId(order.get("id"));
		paymentOrder.setAmount(amountInPaise);
		paymentOrder.setCurrency(request.getCurrency());
		paymentOrder.setReceipt(request.getReceipt());
		paymentOrder.setStatus(PaymentStatus.CREATED);
		paymentOrder.setCustomerName(request.getCustomerName());
		paymentOrder.setCustomerEmail(request.getCustomerEmail());
		paymentOrder.setCustomerPhone(request.getCustomerPhone());

		paymentOrderRepository.save(paymentOrder);

		return new OrderResponse(order.get("id"), amountInPaise, request.getCurrency(), request.getReceipt(),
				CREATED_STATUS, keyId);
	}

	/**
	 * Verifies the payment signature returned by Razorpay Checkout.
	 *
	 * <p>
	 * Signature verification ensures the payment response originated from Razorpay
	 * and has not been tampered with.
	 * </p>
	 *
	 * @param request payment verification request
	 * @return {@code true} if the signature is valid; otherwise {@code false}
	 */
	public boolean verifyPaymentSignature(PaymentVerificationRequest request) {
		String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
		return SignatureUtil.verifySignature(payload, request.getRazorpaySignature(), keySecret);
	}

	/** Retrieves all payment records. */
	public List<PaymentOrder> getAllPayments() {
		return paymentOrderRepository.findAll();
	}

	/**
	 * Retrieves a payment using the database identifier.
	 *
	 * @param id payment ID
	 * @return payment record
	 * @throws RuntimeException if the payment does not exist
	 */
	public PaymentOrder getPaymentById(Long id) {
		return paymentOrderRepository.findById(id).orElseThrow(() -> new RuntimeException(PAYMENT_NOT_FOUND));
	}

	/**
	 * Retrieves a payment using the Razorpay payment ID.
	 *
	 * @param paymentId Razorpay payment ID
	 * @return payment record
	 */
	public PaymentOrder getPaymentByPaymentId(String paymentId) {
		return paymentOrderRepository.findByRazorpayPaymentId(paymentId)
				.orElseThrow(() -> new RuntimeException(PAYMENT_NOT_FOUND));
	}

	/**
	 * Retrieves a payment using the Razorpay order ID.
	 *
	 * @param orderId Razorpay order ID
	 * @return payment record
	 */
	public PaymentOrder getPaymentByOrderId(String orderId) {
		return paymentOrderRepository.findByRazorpayOrderId(orderId)
				.orElseThrow(() -> new RuntimeException(PAYMENT_NOT_FOUND));
	}

	/**
	 * Updates a payment record when a payment failure is reported.
	 *
	 * <p>
	 * If the corresponding payment order exists, its status and failure details are
	 * updated using the information received from Razorpay.
	 * </p>
	 *
	 * @param request payment failure request
	 */
	@Override
	public void updateFailedPayment(PaymentFailureRequest request) {

		paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).ifPresentOrElse(order -> {

			if (PaymentStatus.FAILED.equals(order.getStatus())) {

				log.info("Payment already marked as FAILED. OrderId={}", order.getRazorpayOrderId());

				return;
			}

			updateFailureDetails(order, request);

		}, () -> log.warn("Payment order not found for Razorpay order ID: {}", request.getRazorpayOrderId()));
	}

	/**
	 * Updates the payment entity with failure details.
	 *
	 * @param order   existing payment order
	 * @param request payment failure information
	 */
	private void updateFailureDetails(PaymentOrder order, PaymentFailureRequest request) {

		order.setStatus(PaymentStatus.FAILED);
		order.setRazorpayPaymentId(request.getRazorpayPaymentId());
		order.setFailureReason(request.getFailureReason());

		paymentOrderRepository.save(order);

		log.info("Payment marked as FAILED. OrderId={}, PaymentId={}", order.getRazorpayOrderId(),
				order.getRazorpayPaymentId());
	}
}
