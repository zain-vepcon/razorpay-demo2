package com.example.razorpay.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.razorpay.dto.ApiResponse;

/**
 * Global exception handler for REST APIs.
 *
 * <p>
 * Centralizes exception handling across all controllers and returns
 * standardized API responses for validation and unexpected errors.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handles bean validation failures.
	 *
	 * @param exception validation exception
	 * @return validation error response
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {

		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));

		LOGGER.warn("Validation failed: {}", message);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
	}

	/**
	 * Handles unexpected exceptions.
	 *
	 * @param exception unexpected exception
	 * @return generic error response
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception exception) {

		LOGGER.error("Unhandled exception occurred.", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail("An unexpected error occurred."));
	}
}