package com.geekup.concert.voucher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 WHERE v.id = :id AND v.usedCount < v.maxUses")
    int incrementUsedCount(@Param("id") Long id);

    @Query("SELECT v FROM Voucher v WHERE (:code IS NULL OR v.code LIKE %:code%) AND (:status IS NULL OR v.status = :status)")
    Page<Voucher> searchVouchers(@Param("code") String code, @Param("status") Voucher.VoucherStatus status, Pageable pageable);
}
