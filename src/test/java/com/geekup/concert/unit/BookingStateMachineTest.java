package com.geekup.concert.unit;

import com.geekup.concert.booking.BookingStateMachine;
import com.geekup.concert.booking.BookingStatus;
import com.geekup.concert.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingStateMachineTest {

    @Test
    void shouldAllowPendingPaymentToPaid() {
        assertDoesNotThrow(() -> BookingStateMachine.validate(
                BookingStatus.PENDING_PAYMENT, BookingStatus.PAID));
    }

    @Test
    void shouldAllowPendingPaymentToFailed() {
        assertDoesNotThrow(() -> BookingStateMachine.validate(
                BookingStatus.PENDING_PAYMENT, BookingStatus.FAILED));
    }

    @Test
    void shouldAllowPendingPaymentToCancelled() {
        assertDoesNotThrow(() -> BookingStateMachine.validate(
                BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED));
    }

    @Test
    void shouldAllowPendingPaymentToExpired() {
        assertDoesNotThrow(() -> BookingStateMachine.validate(
                BookingStatus.PENDING_PAYMENT, BookingStatus.EXPIRED));
    }

    @Test
    void shouldAllowPaidToRefunded() {
        assertDoesNotThrow(() -> BookingStateMachine.validate(
                BookingStatus.PAID, BookingStatus.REFUNDED));
    }

    @Test
    void shouldRejectPaidToCancelled() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStateMachine.validate(BookingStatus.PAID, BookingStatus.CANCELLED));
        assertEquals("INVALID_STATE_TRANSITION", ex.getCode());
    }

    @Test
    void shouldRejectFailedToAny() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStateMachine.validate(BookingStatus.FAILED, BookingStatus.PAID));
        assertEquals("INVALID_STATE_TRANSITION", ex.getCode());
    }

    @Test
    void shouldRejectExpiredToAny() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStateMachine.validate(BookingStatus.EXPIRED, BookingStatus.PAID));
        assertEquals("INVALID_STATE_TRANSITION", ex.getCode());
    }

    @Test
    void shouldRejectRefundedToAny() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStateMachine.validate(BookingStatus.REFUNDED, BookingStatus.PAID));
        assertEquals("INVALID_STATE_TRANSITION", ex.getCode());
    }
}
