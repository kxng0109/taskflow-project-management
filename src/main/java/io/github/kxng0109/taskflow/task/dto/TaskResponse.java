package io.github.kxng0109.taskflow.task.dto;

import io.github.kxng0109.taskflow.project.dto.UserSummaryResponse;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String status,
        UserSummaryResponse assignee
) {
}
