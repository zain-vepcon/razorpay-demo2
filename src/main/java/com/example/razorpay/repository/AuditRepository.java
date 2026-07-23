package com.example.razorpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.razorpay.model.AuditLog;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {

}
