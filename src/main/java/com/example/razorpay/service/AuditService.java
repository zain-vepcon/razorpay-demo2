package com.example.razorpay.service;

import com.example.razorpay.dto.PaymentEvent;

public interface AuditService {

	void saveAudit(PaymentEvent event);

}
