package com.example.razorpay.service.Impl;

import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.SmsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

	@Override
	public void sendSMS(PaymentEvent event) {
		log.info("SMS sent for Order={}, Payment={}", event.getOrderId(), event.getPaymentId());
	}

}
