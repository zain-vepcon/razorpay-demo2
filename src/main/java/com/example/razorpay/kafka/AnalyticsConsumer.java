package com.example.razorpay.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {
	private final AnalyticsService analyticsService;

	@KafkaListener(topics = TopicNames.PAYMENT_EVENTS, groupId = "analytics-group")
	public void consume(PaymentEvent event) {

		analyticsService.updateDashboard(event);

	}

}
