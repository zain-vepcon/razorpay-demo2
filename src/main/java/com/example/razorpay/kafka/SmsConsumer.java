package com.example.razorpay.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.SmsService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SmsConsumer {
	private final SmsService smsService;

	@KafkaListener(topics = TopicNames.PAYMENT_EVENTS, groupId = "sms-group")
	public void consume(PaymentEvent event) {

		smsService.sendSMS(event);

	}

}
