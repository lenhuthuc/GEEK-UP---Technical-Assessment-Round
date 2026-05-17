package com.geekup.concert.voucher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Voucher preview request")
public class VoucherPreviewRequest {

    @NotBlank
    @Schema(description = "Voucher code", example = "SUMMER10")
    private String voucherCode;

    @NotNull
    @Schema(description = "Subtotal amount of the order", example = "4500000")
    private BigDecimal subtotal;
}
