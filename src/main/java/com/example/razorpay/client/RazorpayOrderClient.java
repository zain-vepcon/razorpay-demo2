package com.example.razorpay.client;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client wrapper responsible for interacting with Razorpay Order APIs.
 *
 * <p>
 * This class acts as an abstraction layer between the application business
 * logic and the Razorpay SDK. Wrapping the third-party SDK improves testability
 * and prevents the service layer from directly depending on external
 * implementation details.
 * </p>
 *
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Create Razorpay orders</li>
 * <li>Encapsulate Razorpay SDK communication</li>
 * <li>Provide a mockable interface for unit testing</li>
 * </ul>
 *
 * @author Zain
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayOrderClient {

	/**
	 * Razorpay SDK client used for communicating with Razorpay APIs.
	 */
	private final RazorpayClient razorpayClient;

	/**
	 * Creates a new order in Razorpay.
	 *
	 * <p>
	 * Razorpay expects the request payload as a JSON object containing order
	 * details such as amount, currency, and receipt.
	 * </p>
	 *
	 * @param request JSON request payload required by Razorpay
	 *
	 * @return created Razorpay order response
	 *
	 * @throws RazorpayException if Razorpay API fails while creating the order
	 */
	public Order createOrder(JSONObject request) throws RazorpayException {
		log.debug("Sending create order request to Razorpay");

		return razorpayClient.orders.create(request);
	}

}