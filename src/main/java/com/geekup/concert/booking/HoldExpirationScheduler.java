package com.geekup.concert.booking;

import com.geekup.concert.ticket.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Sweeps expired holds every 60 seconds.
 * Uses SELECT ... FOR UPDATE SKIP LOCKED so multiple instances can run safely.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireHolds() {
        List<Booking> expired = bookingRepository.findExpiredHolds(Instant.now(), 100);

        if (expired.isEmpty()) return;

        log.info("Found {} expired holds to process", expired.size());

        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.EXPIRED);
            ticketCategoryRepository.releaseQuantities(booking.getId());
            bookingRepository.save(booking);
            log.info("Expired booking {} - tickets returned to inventory", booking.getId());
        }
    }
}
