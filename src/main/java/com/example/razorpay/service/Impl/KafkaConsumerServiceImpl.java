package com.example.razorpay.service.Impl;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.kafka.TopicNames;
import com.example.razorpay.repository.PaymentOrderRepository;
import com.example.razorpay.service.AnalyticsService;
import com.example.razorpay.service.EmailService;
import com.example.razorpay.service.KafkaConsumerService;
import com.example.razorpay.service.SmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

	private final PaymentOrderRepository repository;
	private final EmailService emailService;

	private final SmsService smsService;

	private final AnalyticsService analyticsService;

//    	private final AuditService auditService;

	@Override
	@KafkaListener(topics = TopicNames.PAYMENT_EVENTS, groupId = "payment-group")
	public void consume(PaymentEvent event) {
		log.info("Received Event {}", event);

		emailService.sendMail(event);

		smsService.sendSMS(event);

		analyticsService.updateDashboard(event);

//		auditService.saveAudit(event);

		repository.findByRazorpayOrderId(event.getOrderId()).ifPresent(order -> {
			order.setStatus(event.getStatus());
			order.setRazorpayPaymentId(event.getPaymentId());
			repository.save(order);
		});
	}

}
