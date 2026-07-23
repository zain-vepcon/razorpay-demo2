package com.example.razorpay.service.Impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.model.AuditLog;
import com.example.razorpay.repository.AuditRepository;
import com.example.razorpay.service.AuditService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

	private final AuditRepository auditRepository;

	@Override
	public void saveAudit(PaymentEvent event) {

		try {

			AuditLog audit = AuditLog.builder().orderId(event.getOrderId()).paymentId(event.getPaymentId())
					.amount(event.getAmount()).receipt(event.getReceipt()).status(event.getStatus())
					.eventType(event.getEventType()).createdAt(LocalDateTime.now()).build();

			auditRepository.save(audit);

			System.out.println("AUDIT SAVED SUCCESSFULLY");

		} catch (Exception ex) {

			ex.printStackTrace();

		}
	}

}
