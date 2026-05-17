package com.geekup.concert.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payment callback from gateway (mock)")
public class PaymentCallbackRequest {

    @NotBlank
    @Schema(description = "Provider transaction ID", example = "vnp_98765")
    private String providerTxnId;

    @NotNull
    @Schema(description = "Booking ID")
    private Long bookingId;

    @NotBlank
    @Schema(description = "Payment status: SUCCESS or FAILED", example = "SUCCESS")
    private String status;

    @NotNull
    @Schema(description = "Payment amount", example = "2400000")
    private BigDecimal amount;

    @Schema(description = "Payment provider", example = "VNPAY")
    private String provider;

    @Schema(description = "HMAC signature for verification")
    private String signature;
}
