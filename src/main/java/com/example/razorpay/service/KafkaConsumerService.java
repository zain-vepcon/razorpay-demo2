package com.example.razorpay.service;

import com.example.razorpay.dto.PaymentEvent;

public interface KafkaConsumerService {

	void consume(PaymentEvent event);

}
