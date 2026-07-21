package com.example.razorpay.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.OrderResponse;
import com.example.razorpay.exception.PaymentNotFoundException;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayException;

/**
 * Unit tests for {@link PaymentController}.
 *
 * <p>
 * Validates REST API behaviour of payment operations including:
 * </p>
 *
 * <ul>
 * <li>Creating Razorpay orders</li>
 * <li>Payment signature verification</li>
 * <li>Fetching payment records</li>
 * </ul>
 *
 * <p>
 * PaymentService is mocked to isolate controller behaviour.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PaymentService paymentService;

	@Autowired
	private ObjectMapper objectMapper;

	private PaymentOrder paymentOrder;

	@BeforeEach
	void setup() {

		paymentOrder = new PaymentOrder();
		paymentOrder.setId(1L);
		paymentOrder.setRazorpayOrderId("order_123");
		paymentOrder.setRazorpayPaymentId("pay_123");
	}

	/**
	 * Tests successful order creation.
	 */
	@Test
	void createOrder_success() throws Exception {

		OrderRequest request = new OrderRequest();
		request.setAmount(500.0);
		request.setCurrency("INR");
		request.setReceipt("receipt_1");
		request.setCustomerName("Mohammed Zain");
		request.setCustomerEmail("zain@test.com");
		request.setCustomerPhone("8019242936");

		OrderResponse response = new OrderResponse("order_123", 50000L, "INR", "receipt_1", "CREATED", "rzp_test");

		when(paymentService.createOrder(any(OrderRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/payments/create-order").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.message").value("Order created"));
	}

	/**
	 * Tests Razorpay exception.
	 */
	@Test
	void createOrder_failure() throws Exception {

		OrderRequest request = new OrderRequest();
		request.setAmount(500.0);
		request.setCurrency("INR");
		request.setReceipt("receipt_1");
		request.setCustomerName("Mohammed Zain");
		request.setCustomerEmail("zain@test.com");
		request.setCustomerPhone("8019242936");

		when(paymentService.createOrder(any(OrderRequest.class))).thenThrow(new RazorpayException("Razorpay down"));

		mockMvc.perform(post("/api/payments/create-order").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Could not create order: Razorpay down"));
	}

	/**
	 * Validation failure.
	 */
	@Test
	void createOrder_validationFailure() throws Exception {

		OrderRequest request = new OrderRequest();

		request.setCurrency("INR");
		request.setReceipt("receipt_1");

		mockMvc.perform(post("/api/payments/create-order").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	/**
	 * Signature verification success.
	 */
	@Test
	void verifyPayment_success() throws Exception {

		when(paymentService.verifyPaymentSignature(any())).thenReturn(true);

		String body = """
				{
				  "razorpayOrderId":"order_123",
				  "razorpayPaymentId":"pay_123",
				  "razorpaySignature":"signature"
				}
				""";

		mockMvc.perform(post("/api/payments/verify").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Signature verified"));
	}

	/**
	 * Invalid signature.
	 */
	@Test
	void verifyPayment_failed() throws Exception {

		when(paymentService.verifyPaymentSignature(any())).thenReturn(false);

		String body = """
				{
				  "razorpayOrderId":"order_123",
				  "razorpayPaymentId":"pay_123",
				  "razorpaySignature":"invalid"
				}
				""";

		mockMvc.perform(post("/api/payments/verify").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Signature verification failed"));
	}

	/**
	 * Get all payments.
	 */
	@Test
	void getAllPayments_success() throws Exception {

		when(paymentService.getAllPayments()).thenReturn(List.of(paymentOrder));

		mockMvc.perform(get("/api/payments")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Payment fetched successfully"));
	}

	/**
	 * Get payment by id.
	 */
	@Test
	void getPaymentById_success() throws Exception {

		when(paymentService.getPaymentById(1L)).thenReturn(paymentOrder);

		mockMvc.perform(get("/api/payments/1")).andExpect(status().isOk());
	}

	/**
	 * Get payment by payment id.
	 */
	@Test
	void getByPaymentId_success() throws Exception {

		when(paymentService.getPaymentByPaymentId("pay_123")).thenReturn(paymentOrder);

		mockMvc.perform(get("/api/payments/payment-id/pay_123")).andExpect(status().isOk());
	}

	/**
	 * Get payment by order id.
	 */
	@Test
	void getByOrderId_success() throws Exception {

		when(paymentService.getPaymentByOrderId("order_123")).thenReturn(paymentOrder);

		mockMvc.perform(get("/api/payments/order/order_123")).andExpect(status().isOk());
	}

	/**
	 * Payment not found.
	 */
	@Test
	void getPaymentById_notFound() throws Exception {

		when(paymentService.getPaymentById(100L)).thenThrow(new PaymentNotFoundException("Payment not found."));

		mockMvc.perform(get("/api/payments/100")).andExpect(status().isNotFound());
	}

	@Test
	void paymentFailed_success() throws Exception {

		String body = """
				{
				  "razorpayOrderId":"order_123",
				  "razorpayPaymentId":"pay_123",
				  "failureReason":"Payment declined by bank"
				}
				""";

		mockMvc.perform(post("/api/payments/failed").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Payment failure recorded"));

		verify(paymentService).updateFailedPayment(any());
	}

}