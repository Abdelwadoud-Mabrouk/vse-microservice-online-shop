package com.vseonlineshop.payment.repository;

import com.vseonlineshop.payment.entity.Payment;
import com.vseonlineshop.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    @Query("SELECT pt.payment FROM PaymentTransaction pt WHERE pt.providerTransactionId = :providerTransactionId")
    Optional<Payment> findPaymentByProviderTransactionId(@Param("providerTransactionId") String providerTransactionId);
    List<PaymentTransaction> findPaymentTransactionsByPayment(Payment payment);
    Optional<PaymentTransaction> findPaymentTransactionsByProviderTransactionIdAndCaptureIdNotNull(String previousTransactionId);
}

