package com.geekup.concert.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Hold booking request")
public class HoldRequest {

    @NotNull
    @Schema(description = "Concert ID")
    private Long concertId;

    @NotEmpty
    @Valid
    @Schema(description = "Ticket items to hold")
    private List<HoldItem> items;

    @Schema(description = "Optional voucher code", example = "SUMMER10")
    private String voucherCode;

    @Data
    public static class HoldItem {
        @NotNull
        @Schema(description = "Ticket category ID")
        private Long ticketCategoryId;

        @NotNull
        @Min(1)
        @Schema(description = "Quantity", example = "2")
        private Integer quantity;
    }
}
