package com.example.razorpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.razorpay.model.PaymentOrder;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
	Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);

	Optional<PaymentOrder> findByRazorpayPaymentId(String razorpayPaymentId);

}
