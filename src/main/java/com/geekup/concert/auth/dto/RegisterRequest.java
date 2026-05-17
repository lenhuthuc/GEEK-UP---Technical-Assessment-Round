package com.geekup.concert.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @NotBlank
    @Email
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "Password (min 6 characters)", example = "User@123")
    private String password;

    @NotBlank
    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Phone number", example = "0901234567")
    private String phone;
}
