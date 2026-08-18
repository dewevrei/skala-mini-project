package com.dewevrei.aikanban.task;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dewevrei.aikanban.common.time.SeoulTimeMapper;
import com.dewevrei.aikanban.domain.Task;

public record TaskResponse(Long id, Long projectId, Long columnId, String title, String description,
        LocalDate startDate, LocalDate endDate, int priority, long sortOrder,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getColumnId(), task.getTitle(),
                task.getDescription(), task.getStartDate(), task.getEndDate(), task.getPriority(),
                task.getSortOrder(), SeoulTimeMapper.toApiTimestamp(task.getCreatedAt()),
                SeoulTimeMapper.toApiTimestamp(task.getUpdatedAt()));
    }
}
