package com.geekup.concert.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT b.* FROM bookings b
            WHERE b.status = 'PENDING_PAYMENT'
            AND b.hold_expires_at < :now
            ORDER BY b.hold_expires_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Booking> findExpiredHolds(@Param("now") Instant now, @Param("limit") int limit);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE (:status IS NULL OR b.status = :status)
            AND (:concertId IS NULL OR b.concertId = :concertId)
            AND (:userId IS NULL OR b.userId = :userId)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> searchBookings(
            @Param("status") BookingStatus status,
            @Param("concertId") Long concertId,
            @Param("userId") Long userId,
            Pageable pageable);
}
