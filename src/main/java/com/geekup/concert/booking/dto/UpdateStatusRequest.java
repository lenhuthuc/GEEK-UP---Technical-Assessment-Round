package com.geekup.concert.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Update booking status request")
public class UpdateStatusRequest {

    @NotBlank
    @Schema(description = "New booking status", example = "CANCELLED")
    private String status;

    @Schema(description = "Reason for status change")
    private String reason;
}
