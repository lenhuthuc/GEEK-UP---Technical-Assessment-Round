package com.geekup.concert.voucher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(description = "Create voucher request")
public class CreateVoucherRequest {

    @NotBlank
    @Schema(description = "Voucher code", example = "SUMMER10")
    private String code;

    @NotBlank
    @Schema(description = "Voucher type: PERCENT or FIXED", example = "PERCENT")
    private String type;

    @NotNull
    @Schema(description = "Discount value (percentage or fixed amount)", example = "10")
    private BigDecimal value;

    @Schema(description = "Minimum order amount to apply", example = "1000000")
    private BigDecimal minOrderAmount;

    @Schema(description = "Maximum discount amount (for PERCENT type)", example = "500000")
    private BigDecimal maxDiscountAmount;

    @NotNull
    @Schema(description = "Maximum total uses", example = "100")
    private Integer maxUses;

    @Schema(description = "Maximum uses per user", example = "2")
    private Integer maxUsesPerUser = 1;

    @NotNull
    @Schema(description = "Valid from date")
    private Instant validFrom;

    @NotNull
    @Schema(description = "Valid until date")
    private Instant validUntil;
}
