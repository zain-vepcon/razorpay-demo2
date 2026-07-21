package com.example.razorpay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.razorpay.model.PaymentOrder;

/**
 * Repository for managing {@link PaymentOrder} entities.
 *
 * <p>
 * Provides CRUD operations along with custom finder methods for retrieving
 * payment orders using Razorpay order and payment identifiers.
 * </p>
 *
 * @author Zain
 * @since 1.0
 */
//@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

	/**
	 * Retrieves a payment order using the Razorpay order ID.
	 *
	 * @param razorpayOrderId Razorpay order identifier
	 * @return matching payment order if found
	 */
	Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);

	/**
	 * Retrieves a payment order using the Razorpay payment ID.
	 *
	 * @param razorpayPaymentId Razorpay payment identifier
	 * @return matching payment order if found
	 */
	Optional<PaymentOrder> findByRazorpayPaymentId(String razorpayPaymentId);

}