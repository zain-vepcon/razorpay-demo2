package com.example.razorpay.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailConsumer {
	private final EmailService emailService;

	@KafkaListener(topics = TopicNames.PAYMENT_EVENTS, groupId = "email-group")
	public void consume(PaymentEvent event) {

		System.out.println("EMAIL CONSUMER RECEIVED");

		emailService.sendMail(event);
	}

}
