package com.geekup.concert.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByProviderTxnId(String providerTxnId);
    Optional<Payment> findByBookingId(Long bookingId);
}
