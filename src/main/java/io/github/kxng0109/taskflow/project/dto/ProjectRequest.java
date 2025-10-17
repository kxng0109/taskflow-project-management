package io.github.kxng0109.taskflow.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "Name cannot be blank")
        String name,
        String description
) {
}
