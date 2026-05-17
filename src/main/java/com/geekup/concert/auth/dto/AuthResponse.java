package com.geekup.concert.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Authentication response with JWT tokens")
public class AuthResponse {

    @Schema(description = "Access token (15 min expiry)")
    private String accessToken;

    @Schema(description = "Refresh token (7 day expiry)")
    private String refreshToken;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User role")
    private String role;
}
