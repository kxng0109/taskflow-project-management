package io.github.kxng0109.taskflow.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Must be a valid email format")
        String email,

        @NotBlank(message = "Password can not be empty")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}
