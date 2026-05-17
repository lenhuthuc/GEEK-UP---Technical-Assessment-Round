package com.geekup.concert.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Create concert request")
public class CreateConcertRequest {

    @NotBlank
    @Schema(description = "Concert title", example = "Rock Festival 2026")
    private String title;

    @Schema(description = "Concert description")
    private String description;

    @Schema(description = "Venue name and address", example = "National Stadium, Hanoi")
    private String venue;

    @NotNull
    @Schema(description = "Start time (UTC)")
    private Instant startsAt;

    @NotNull
    @Schema(description = "End time (UTC)")
    private Instant endsAt;

    @Schema(description = "Cover image URL")
    private String coverImageUrl;
}
