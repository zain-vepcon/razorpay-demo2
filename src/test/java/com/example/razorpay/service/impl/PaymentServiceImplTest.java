package com.example.razorpay.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.razorpay.client.RazorpayOrderClient;
import com.example.razorpay.dto.OrderRequest;
import com.example.razorpay.dto.PaymentFailureRequest;
import com.example.razorpay.dto.PaymentVerificationRequest;
import com.example.razorpay.enums.PaymentStatus;
import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.service.EmailService;
import com.example.razorpay.service.PaymentService;
import com.example.razorpay.service.Impl.PaymentServiceImpl;
import com.example.razorpay.util.SignatureUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayException;

/**
 * Unit tests for {@link PaymentService}.
 *
 * <p>
 * This test suite validates:
 * </p>
 *
 * <ul>
 * <li>Successful Razorpay order creation</li>
 * <li>Razorpay order creation failures</li>
 * <li>Payment signature verification</li>
 * <li>Payment retrieval operations</li>
 * <li>Payment failure handling</li>
 * <li>Repository interaction behaviour</li>
 * </ul>
 *
 * <p>
 * External dependencies such as Razorpay APIs and database repositories are
 * mocked using Mockito.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	@Mock
	private RazorpayOrderClient razorpayOrderClient;

	@Mock
	private PaymentOrderRepository repository;

	@Mock
	private EmailService emailService;

	private PaymentServiceImpl service;

	@BeforeEach
	void setup() throws Exception {

		service = new PaymentServiceImpl(emailService, razorpayOrderClient, repository);

		setField("keyId", "rzp_test_123");
		setField("keySecret", "secret");
	}

	private void setField(String name, Object value) throws Exception {

		Field field = PaymentServiceImpl.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(service, value);

	}

	@Test
	void createOrderSuccess() throws Exception {

		OrderRequest request = new OrderRequest();
		request.setAmount(100d);
		request.setCurrency("INR");
		request.setReceipt("REC001");

		Order order = mock(Order.class);

		when(order.get("id")).thenReturn("order_123");

		when(razorpayOrderClient.createOrder(any(JSONObject.class))).thenReturn(order);

		var response = service.createOrder(request);

		assertEquals("order_123", response.getRazorpayOrderId());
		assertEquals(10000L, response.getAmount());
		assertEquals("INR", response.getCurrency());

		verify(repository).save(any(PaymentOrder.class));
	}

	@Test
	void createOrderThrowsException() throws Exception {

		OrderRequest request = new OrderRequest();
		request.setAmount(100d);

		when(razorpayOrderClient.createOrder(any(JSONObject.class))).thenThrow(new RazorpayException("error"));

		assertThrows(RazorpayException.class, () -> service.createOrder(request));
	}

	@Test
	void verifyPaymentSignatureTrue() {

		PaymentVerificationRequest request = new PaymentVerificationRequest();

		request.setRazorpayOrderId("order");
		request.setRazorpayPaymentId("payment");
		request.setRazorpaySignature("signature");

		try (MockedStatic<SignatureUtil> mocked = mockStatic(SignatureUtil.class)) {

			mocked.when(() -> SignatureUtil.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

			assertTrue(service.verifyPaymentSignature(request));
		}
	}

	@Test
	void verifyPaymentSignatureFalse() {

		PaymentVerificationRequest request = new PaymentVerificationRequest();

		request.setRazorpayOrderId("order");
		request.setRazorpayPaymentId("payment");
		request.setRazorpaySignature("signature");

		try (MockedStatic<SignatureUtil> mocked = mockStatic(SignatureUtil.class)) {

			mocked.when(() -> SignatureUtil.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

			assertFalse(service.verifyPaymentSignature(request));
		}
	}

	@Test
	void getAllPayments() {

		when(repository.findAll()).thenReturn(Arrays.asList(new PaymentOrder(), new PaymentOrder()));

		assertEquals(2, service.getAllPayments().size());
	}

	@Test
	void getPaymentByIdFound() {

		PaymentOrder order = new PaymentOrder();

		when(repository.findById(1L)).thenReturn(Optional.of(order));

		assertEquals(order, service.getPaymentById(1L));
	}

	@Test
	void getPaymentByIdNotFound() {

		when(repository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.getPaymentById(1L));
	}

	@Test
	void getPaymentByPaymentIdFound() {

		PaymentOrder order = new PaymentOrder();

		when(repository.findByRazorpayPaymentId("pay")).thenReturn(Optional.of(order));

		assertEquals(order, service.getPaymentByPaymentId("pay"));
	}

	@Test
	void getPaymentByPaymentIdNotFound() {

		when(repository.findByRazorpayPaymentId("pay")).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.getPaymentByPaymentId("pay"));
	}

	@Test
	void getPaymentByOrderIdFound() {

		PaymentOrder order = new PaymentOrder();

		when(repository.findByRazorpayOrderId("order")).thenReturn(Optional.of(order));

		assertEquals(order, service.getPaymentByOrderId("order"));
	}

	@Test
	void getPaymentByOrderIdNotFound() {

		when(repository.findByRazorpayOrderId("order")).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.getPaymentByOrderId("order"));
	}

	@Test
	void updateFailedPaymentFound() {

		PaymentOrder order = new PaymentOrder();

		PaymentFailureRequest request = new PaymentFailureRequest();

		request.setRazorpayOrderId("order");
		request.setRazorpayPaymentId("pay");
		request.setErrorReason("DECLINED");
		request.setFailureReason("Payment declined by bank");

		when(repository.findByRazorpayOrderId("order")).thenReturn(Optional.of(order));

		service.updateFailedPayment(request);

		assertEquals(PaymentStatus.FAILED, order.getStatus());
		assertEquals("pay", order.getRazorpayPaymentId());
		assertEquals("Payment declined by bank", order.getFailureReason());

		verify(repository).save(order);
	}

	@Test
	void updateFailedPaymentNotFound() {

		PaymentFailureRequest request = new PaymentFailureRequest();

		request.setRazorpayOrderId("order");

		when(repository.findByRazorpayOrderId("order")).thenReturn(Optional.empty());

		service.updateFailedPayment(request);

		verify(repository, never()).save(any());
	}

}