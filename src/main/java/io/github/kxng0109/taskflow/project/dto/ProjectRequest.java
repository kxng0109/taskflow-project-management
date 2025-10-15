package io.github.kxng0109.taskflow.project.dto;

import jakarta.validation.constraints.NotNull;

public record ProjectRequest(
        @NotNull(message = "Name cannot be blank")
        String name,
        String description
) {
}
