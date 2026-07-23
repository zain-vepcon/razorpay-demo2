package com.example.razorpay.service.Impl;

import org.springframework.stereotype.Service;

import com.example.razorpay.dto.PaymentEvent;
import com.example.razorpay.service.InvoiceService;

@Service
public class InvoiceServiceImpl implements InvoiceService {
	@Override
	public void generateInvoice(PaymentEvent event) {
		// invoice logic
	}

}

//@Override
//public void saveAudit(PaymentEvent event){
//
//    Audit audit = new Audit();
//
//    audit.setOrderId(event.getOrderId());
//
//    audit.setPaymentId(event.getPaymentId());
//
//    audit.setStatus(event.getStatus().name());
//
//    auditRepository.save(audit);
//
//}