package com.example.razorpay.service.Impl;

import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.AnalyticsService;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

	@Override
	public void updateDashboard(PaymentEvent event) {
		System.out.println("Analytics Updated");

		System.out.println(event.getAmount());
	}

}
