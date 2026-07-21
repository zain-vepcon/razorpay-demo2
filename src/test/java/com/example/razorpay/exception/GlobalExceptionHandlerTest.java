package com.example.razorpay.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.razorpay.dto.ApiResponse;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * @author Zain
 * @since 1.0
 */
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Test
	void handlePaymentNotFound_shouldReturn404() {

		PaymentNotFoundException exception = new PaymentNotFoundException("Payment not found.");

		ResponseEntity<ApiResponse<Object>> response = handler.handlePaymentNotFound(exception);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

	}

	@Test
	void handleGeneric_shouldReturn500() {

		ResponseEntity<ApiResponse<Object>> response = handler.handleGeneric(new RuntimeException("error"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

	}

	@Test
	void handleValidation_shouldReturn400() {

		BindingResult bindingResult = mock(BindingResult.class);

		FieldError error = new FieldError("orderRequest", "amount", "must not be null");

		when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ApiResponse<Object>> response = handler.handleValidation(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

	}

	@Test
	void shouldHandleFaviconNotFound() {

		NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "favicon.ico");

		ResponseEntity<Void> response = handler.handleResourceNotFound(exception);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

	}

	@Test
	void shouldHandleOtherStaticResourceNotFound() {

		NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "abc.js");

		ResponseEntity<Void> response = handler.handleResourceNotFound(exception);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void shouldHandleGenericException() {

		Exception exception = new RuntimeException("Database error");

		ResponseEntity<ApiResponse<Object>> response = handler.handleGeneric(exception);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

		assertNotNull(response.getBody());

	}

	@Test
	void shouldHandleValidationException() {

		FieldError fieldError = new FieldError("payment", "amount", "Amount is required");

		BindingResult bindingResult = mock(BindingResult.class);

		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ApiResponse<Object>> response = handler.handleValidation(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		assertNotNull(response.getBody());

	}

}