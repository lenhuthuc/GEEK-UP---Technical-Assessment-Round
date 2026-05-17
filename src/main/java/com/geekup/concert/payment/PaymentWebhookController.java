package com.geekup.concert.payment;

import com.geekup.concert.booking.BookingService;
import com.geekup.concert.booking.dto.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment webhook endpoints (mock)")
public class PaymentWebhookController {

    private final BookingService bookingService;

    @PostMapping("/payment")
    @Operation(summary = "Mock payment callback (simulates VNPay/MoMo/Stripe webhook)")
    public ResponseEntity<BookingResponse> handlePaymentCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {
        log.info("Received payment callback: booking={}, status={}, txn={}",
                request.getBookingId(), request.getStatus(), request.getProviderTxnId());

        // In production, verify HMAC signature here
        boolean success = "SUCCESS".equalsIgnoreCase(request.getStatus());

        // Use a system user ID for webhook-initiated actions
        // In production, this would validate against the actual booking owner
        BookingResponse response = bookingService.confirmPayment(
                request.getBookingId(),
                null, // webhook doesn't have user context; method handles this
                success,
                request.getProviderTxnId(),
                request.getProvider() != null ? request.getProvider() : "MOCK"
        );

        return ResponseEntity.ok(response);
    }
}
