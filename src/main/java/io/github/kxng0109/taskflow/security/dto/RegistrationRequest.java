package io.github.kxng0109.taskflow.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotNull(message = "Name cannot be empty")
        String name,

        @NotNull(message = "Email cannot be blank")
        @Email(message = "Must be a valid email format")
        String email,

        @NotNull(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
){}
