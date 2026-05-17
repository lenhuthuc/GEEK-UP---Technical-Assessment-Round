package com.geekup.concert.ticket;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByConcertIdOrderBySortOrder(Long concertId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TicketCategory tc WHERE tc.id IN :ids")
    List<TicketCategory> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = """
            UPDATE ticket_categories tc
            SET available_quantity = available_quantity + bi.quantity
            FROM booking_items bi
            WHERE bi.booking_id = :bookingId
            AND tc.id = bi.ticket_category_id
            """, nativeQuery = true)
    void releaseQuantities(@Param("bookingId") Long bookingId);
}
