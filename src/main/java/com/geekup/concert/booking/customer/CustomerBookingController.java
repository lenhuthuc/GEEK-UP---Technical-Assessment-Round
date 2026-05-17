package com.geekup.concert.booking.customer;

import com.geekup.concert.booking.BookingService;
import com.geekup.concert.booking.dto.BookingResponse;
import com.geekup.concert.booking.dto.HoldRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings (Customer)", description = "Customer booking APIs")
public class CustomerBookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    @Operation(summary = "Create a PENDING_PAYMENT booking (hold tickets)")
    public ResponseEntity<BookingResponse> hold(
            @Valid @RequestBody HoldRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.hold(request, idempotencyKey, userId));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Mock payment confirmation")
    public ResponseEntity<BookingResponse> confirmPayment(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(required = false) String providerTxnId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.confirmPayment(id, userId, success, providerTxnId, "MOCK"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking while PENDING_PAYMENT")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId, reason));
    }

    @GetMapping
    @Operation(summary = "List current user's bookings")
    public ResponseEntity<Page<BookingResponse>> listBookings(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.listUserBookings(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking detail")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.getBooking(id, userId));
    }
}
