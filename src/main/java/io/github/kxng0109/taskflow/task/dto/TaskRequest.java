package io.github.kxng0109.taskflow.task.dto;

import io.github.kxng0109.taskflow.task.validation.ValidTaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskRequest(
        @NotNull(message = "Title cannot be blank")
        String title,

        String description,

        @NotBlank(message = "Status cannot be blank")
        @ValidTaskStatus
        String status,

        Long assigneeId
) {
}
