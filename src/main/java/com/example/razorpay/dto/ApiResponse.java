package com.example.razorpay.dto;

import lombok.Getter;

/**
 * Standard API response wrapper used across all REST endpoints.
 *
 * <p>
 * Encapsulates the operation status, response message, and optional response
 * payload to provide a consistent API response structure.
 * </p>
 *
 * @param <T> response payload type
 *
 * @author Zain
 * @since 1.0
 */
@Getter
public final class ApiResponse<T> {

	/**
	 * Indicates whether the request was processed successfully.
	 */
	private final boolean success;

	/**
	 * Response message describing the operation result.
	 */
	private final String message;

	/**
	 * Response payload.
	 */
	private final T data;

	/**
	 * Creates an API response.
	 *
	 * @param success operation status
	 * @param message response message
	 * @param data    response payload
	 */
	private ApiResponse(boolean success, String message, T data) {
		this.success = success;
		this.message = message;
		this.data = data;
	}

	/**
	 * Creates a successful API response.
	 *
	 * @param message success message
	 * @param data    response payload
	 * @param <T>     payload type
	 * @return successful API response
	 */
	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data);
	}

	/**
	 * Creates a failed API response.
	 *
	 * @param message failure message
	 * @param <T>     payload type
	 * @return failed API response
	 */
	public static <T> ApiResponse<T> fail(String message) {
		return new ApiResponse<>(false, message, null);
	}
}
