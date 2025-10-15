package io.github.kxng0109.taskflow.project.dto;

import java.util.List;

public record ProjectResponse(
        long id,
        String name,
        String description,
        List<UserSummaryResponse> members
) {}
