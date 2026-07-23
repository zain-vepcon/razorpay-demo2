package com.example.razorpay.service;

import com.example.razorpay.dto.PaymentEvent;

public interface SmsService {
	void sendSMS(PaymentEvent event);

}
