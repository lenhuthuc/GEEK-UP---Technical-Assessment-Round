package com.geekup.concert.booking.ops;

import com.geekup.concert.booking.BookingService;
import com.geekup.concert.booking.BookingStatus;
import com.geekup.concert.booking.dto.BookingResponse;
import com.geekup.concert.booking.dto.UpdateStatusRequest;
import com.geekup.concert.common.audit.AuditLog;
import com.geekup.concert.common.audit.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Bookings (Operations)", description = "Operator/Admin booking management APIs")
public class OpsBookingController {

    private final BookingService bookingService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Booking dashboard - filter by status, concert, user")
    public ResponseEntity<Page<BookingResponse>> listBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.searchBookings(status, concertId, userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Booking detail with audit log")
    public ResponseEntity<Map<String, Object>> getBookingDetail(@PathVariable Long id) {
        BookingResponse booking = bookingService.getBookingForOps(id);
        Page<AuditLog> auditLogs = auditLogRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("BOOKING", id.toString(), Pageable.ofSize(50));

        Map<String, Object> response = new HashMap<>();
        response.put("booking", booking);
        response.put("auditLogs", auditLogs.getContent());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Manually update booking status (state machine enforced)")
    public ResponseEntity<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication auth) {
        Long actorId = (Long) auth.getPrincipal();
        BookingStatus newStatus = BookingStatus.valueOf(request.getStatus());
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, newStatus, request.getReason(), actorId));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Mark booking as refunded (mock)")
    public ResponseEntity<BookingResponse> refund(
            @PathVariable Long id,
            Authentication auth) {
        Long actorId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.refundBooking(id, actorId));
    }
}
