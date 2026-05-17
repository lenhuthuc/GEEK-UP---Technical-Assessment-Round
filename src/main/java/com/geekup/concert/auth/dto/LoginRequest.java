package com.geekup.concert.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "Email address", example = "user@demo.com")
    private String email;

    @NotBlank
    @Schema(description = "Password", example = "User@123")
    private String password;
}
