package com.geekup.concert.voucher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucherId = :voucherId AND vu.userId = :userId")
    long countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);

    boolean existsByVoucherIdAndBookingId(Long voucherId, Long bookingId);
}
