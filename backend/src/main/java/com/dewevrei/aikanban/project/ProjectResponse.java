package com.dewevrei.aikanban.project;

import java.time.OffsetDateTime;

import com.dewevrei.aikanban.common.time.SeoulTimeMapper;
import com.dewevrei.aikanban.domain.Project;

public record ProjectResponse(Long id, String name, String description,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                SeoulTimeMapper.toApiTimestamp(project.getCreatedAt()),
                SeoulTimeMapper.toApiTimestamp(project.getUpdatedAt()));
    }
}
