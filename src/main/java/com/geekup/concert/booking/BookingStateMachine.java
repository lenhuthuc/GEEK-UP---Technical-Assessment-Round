package com.geekup.concert.booking;

import com.geekup.concert.common.exception.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * Enforces valid booking state transitions.
 *
 * PENDING_PAYMENT -> PAID | FAILED | CANCELLED | EXPIRED
 * PAID            -> REFUNDED
 * All other transitions are rejected with HTTP 422.
 */
public final class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(
            BookingStatus.PENDING_PAYMENT, Set.of(
                    BookingStatus.PAID, BookingStatus.FAILED,
                    BookingStatus.CANCELLED, BookingStatus.EXPIRED),
            BookingStatus.PAID, Set.of(BookingStatus.REFUNDED)
    );

    private BookingStateMachine() {
    }

    public static void validate(BookingStatus from, BookingStatus to) {
        Set<BookingStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BusinessException("INVALID_STATE_TRANSITION",
                    String.format("Cannot transition from %s to %s", from, to));
        }
    }
}
