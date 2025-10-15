package io.github.kxng0109.taskflow.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull(message = "Email cannot be blank")
        @Email(message = "Must be a valid email format")
        String email,

        @NotNull(message = "Password can not be empty")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}
