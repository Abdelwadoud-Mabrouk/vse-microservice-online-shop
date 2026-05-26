package com.vseonlineshop.payment.repository;

import com.vseonlineshop.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByCorrelationId(UUID correlationId);
}
