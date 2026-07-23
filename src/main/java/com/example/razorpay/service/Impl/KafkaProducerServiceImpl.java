package com.example.razorpay.service.Impl;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.kafka.TopicNames;
import com.example.razorpay.service.KafkaProducerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

	private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@Override
	public void publish(PaymentEvent event) {

		System.out.println("Publishing Kafka Event -> " + event);

		kafkaTemplate.send(TopicNames.PAYMENT_EVENTS, event.getOrderId(), event);

	}

}
