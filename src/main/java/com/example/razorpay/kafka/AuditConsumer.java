package com.example.razorpay.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.AuditService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditConsumer {

	private final AuditService auditService;

	@KafkaListener(topics = TopicNames.PAYMENT_EVENTS, groupId = "audit-group")
	public void consume(PaymentEvent event) {

		System.out.println("AUDIT EVENT RECEIVED");
		System.out.println(event);

		auditService.saveAudit(event);
	}

}
