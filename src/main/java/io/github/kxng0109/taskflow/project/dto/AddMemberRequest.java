package io.github.kxng0109.taskflow.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotNull(message = "Email cannot be blank")
        @Email(message = "Must be a valid email format")
        String email
) {
}
