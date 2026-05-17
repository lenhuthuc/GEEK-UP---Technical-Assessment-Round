package com.geekup.concert.voucher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Voucher discount preview response")
public class VoucherPreviewResponse {

    private String code;
    private String type;
    private BigDecimal discountAmount;
    private BigDecimal subtotalBefore;
    private BigDecimal totalAfterDiscount;
    private boolean valid;
    private String message;
}
