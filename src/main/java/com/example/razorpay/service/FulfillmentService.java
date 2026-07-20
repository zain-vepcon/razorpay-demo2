package com.example.razorpay.service;

import org.springframework.stereotype.Service;

import com.example.razorpay.model.PaymentOrder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FulfillmentService {

	public void fulfillOrder(PaymentOrder order) {

		log.info("Generating invoice...");

		log.info("Sending Email...");

		log.info("Sending SMS...");

		log.info("Activating Subscription...");

		log.info("Order fulfilled.");

	}

}
