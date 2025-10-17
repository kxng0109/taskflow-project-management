package io.github.kxng0109.taskflow.task.dto;

import io.github.kxng0109.taskflow.task.validation.ValidTaskStatus;
import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
        @NotBlank(message = "Title cannot be blank")
        String title,

        String description,

        @NotBlank(message = "Status cannot be blank")
        @ValidTaskStatus
        String status,

        Long assigneeId
) {
}
