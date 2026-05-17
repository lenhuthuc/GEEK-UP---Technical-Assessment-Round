package com.geekup.concert.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Booking response")
public class BookingResponse {

    private Long id;
    private Long userId;
    private Long concertId;
    private String concertTitle;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String voucherCode;
    private Instant holdExpiresAt;
    private Instant paidAt;
    private Instant cancelledAt;
    private String cancelledReason;
    private Instant createdAt;
    private List<BookingItemResponse> items;

    @Data
    @Builder
    @AllArgsConstructor
    public static class BookingItemResponse {
        private Long id;
        private Long ticketCategoryId;
        private String ticketCategoryName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
