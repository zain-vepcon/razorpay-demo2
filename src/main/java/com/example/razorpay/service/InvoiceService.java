	package com.example.razorpay.service;

import com.example.razorpay.dto.PaymentEvent;

public interface InvoiceService {
	void generateInvoice(PaymentEvent event);

}
