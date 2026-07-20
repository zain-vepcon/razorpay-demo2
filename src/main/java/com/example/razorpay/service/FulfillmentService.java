package com.example.razorpay.service;

import org.springframework.stereotype.Service;

import com.example.razorpay.model.PaymentOrder;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for post-payment fulfillment activities.
 *
 * <p>
 * Once a payment is successfully captured, this service performs business
 * operations such as invoice generation, customer notification, and service
 * activation.
 * </p>
 *
 * <p>
 * In a production application, each responsibility should typically be
 * delegated to dedicated services (InvoiceService, EmailService,
 * NotificationService, SubscriptionService, etc.).
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
@Slf4j
@Service
public class FulfillmentService {

	/**
	 * Executes post-payment fulfillment operations.
	 *
	 * @param order successfully paid order
	 */
	public void fulfillOrder(PaymentOrder order) {

		log.info("Starting fulfillment for order: {}", order.getRazorpayOrderId());

		generateInvoice();

		sendEmail();

		sendSms();

		activateSubscription();

		log.info("Order {} fulfilled successfully.", order.getRazorpayOrderId());
	}

	/**
	 * Generates the customer invoice.
	 */
	private void generateInvoice() {
		log.info("Generating invoice...");
	}

	/**
	 * Sends payment confirmation email.
	 */
	private void sendEmail() {
		log.info("Sending confirmation email...");
	}

	/**
	 * Sends payment confirmation SMS.
	 */
	private void sendSms() {
		log.info("Sending confirmation SMS...");
	}

	/**
	 * Activates the purchased subscription or service.
	 */
	private void activateSubscription() {
		log.info("Activating subscription...");
	}

}