package com.geekup.concert.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Create ticket category request")
public class CreateTicketCategoryRequest {

    @NotNull
    @Schema(description = "Concert ID")
    private Long concertId;

    @NotBlank
    @Schema(description = "Category name", example = "VIP")
    private String name;

    @NotNull
    @Min(0)
    @Schema(description = "Ticket price", example = "2000000")
    private BigDecimal priceAmount;

    @Schema(description = "Currency code", example = "VND")
    private String priceCurrency = "VND";

    @NotNull
    @Min(1)
    @Schema(description = "Total number of tickets")
    private Integer totalQuantity;

    @Schema(description = "Display sort order")
    private Integer sortOrder = 0;
}
