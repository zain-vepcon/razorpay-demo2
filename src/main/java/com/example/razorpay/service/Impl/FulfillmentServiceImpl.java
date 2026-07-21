package com.example.razorpay.service.Impl;

import org.springframework.stereotype.Service;

import com.example.razorpay.model.PaymentOrder;
import com.example.razorpay.service.EmailService;
import com.example.razorpay.service.FulfillmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link FulfillmentService}.
 *
 * <p>
 * Executes all post-payment business operations after a payment has been
 * successfully captured.
 * </p>
 *
 * <p>
 * Current workflow:
 * <ul>
 * <li>Generate invoice</li>
 * <li>Send payment confirmation email</li>
 * <li>Send SMS notification</li>
 * <li>Activate purchased subscription/service</li>
 * </ul>
 *
 * <p>
 * In production, each responsibility should be delegated to dedicated services
 * such as InvoiceService, EmailService, SmsService, and SubscriptionService.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentServiceImpl implements FulfillmentService {

	private final EmailService emailService;

	/**
	 * Executes all business operations after a successful payment.
	 *
	 * @param order successfully paid order
	 */
	@Override
	public void fulfillOrder(PaymentOrder order) {

		log.info("Starting fulfillment for OrderId={}, PaymentId={}", order.getRazorpayOrderId(),
				order.getRazorpayPaymentId());

		generateInvoice(order);

		sendEmail(order);

		sendSms(order);

		activateSubscription(order);

		log.info("Fulfillment completed successfully for OrderId={}", order.getRazorpayOrderId());
	}

	/**
	 * Generates the customer invoice.
	 *
	 * @param order payment order
	 */
	private void generateInvoice(PaymentOrder order) {

		log.info("Generating invoice for OrderId={}", order.getRazorpayOrderId());

		// TODO:
		// Generate PDF invoice
		// Store invoice
		// Upload to S3 / Cloud Storage
	}

	/**
	 * Sends the payment confirmation email.
	 *
	 * @param order payment order
	 */
	private void sendEmail(PaymentOrder order) {

		log.info("Sending payment confirmation email...");

		emailService.sendPaymentSuccessEmail(order);
	}

	/**
	 * Sends the payment confirmation SMS.
	 *
	 * @param order payment order
	 */
	private void sendSms(PaymentOrder order) {

		log.info("Sending SMS notification for OrderId={}", order.getRazorpayOrderId());

		// TODO:
		// Integrate Twilio / MSG91
	}

	/**
	 * Activates the purchased subscription or service.
	 *
	 * @param order payment order
	 */
	private void activateSubscription(PaymentOrder order) {

		log.info("Activating subscription for OrderId={}", order.getRazorpayOrderId());

		// TODO:
		// Update database
		// Enable premium access
	}

	@Override
	public void handlePaymentFailure(PaymentOrder order) {

		log.info("Starting failure workflow for OrderId={}", order.getRazorpayOrderId());

		sendFailureEmail(order);

		sendFailureSms(order);

		log.info("Failure workflow completed for OrderId={}", order.getRazorpayOrderId());
	}

	/**
	 * Sends payment failure email.
	 *
	 * @param order failed payment order
	 */
	private void sendFailureEmail(PaymentOrder order) {

		log.info("Sending payment failure email...");

		emailService.sendPaymentFailureEmail(order);
	}

	/**
	 * Sends payment failure SMS.
	 *
	 * @param order failed payment order
	 */
	private void sendFailureSms(PaymentOrder order) {

		log.info("Sending payment failure SMS for OrderId={}", order.getRazorpayOrderId());

		// TODO Integrate SMS provider
	}

}