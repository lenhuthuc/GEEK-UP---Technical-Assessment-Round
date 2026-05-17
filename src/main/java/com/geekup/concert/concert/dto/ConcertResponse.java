package com.geekup.concert.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Concert detail response")
public class ConcertResponse {

    private Long id;
    private String title;
    private String description;
    private String venue;
    private Instant startsAt;
    private Instant endsAt;
    private String status;
    private String coverImageUrl;
    private Instant createdAt;
    private List<TicketCategoryResponse> ticketCategories;

    @Data
    @Builder
    @AllArgsConstructor
    public static class TicketCategoryResponse {
        private Long id;
        private String name;
        private java.math.BigDecimal priceAmount;
        private String priceCurrency;
        private Integer totalQuantity;
        private Integer availableQuantity;
    }
}
