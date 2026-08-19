package com.dewevrei.aikanban.aitask;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ErrorCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;
import com.dewevrei.aikanban.task.TaskResponse;

@Service
public class AiTaskPersistence {
    private final ProjectRepository projects;
    private final BoardColumnRepository columns;
    private final TaskRepository tasks;

    public AiTaskPersistence(ProjectRepository projects, BoardColumnRepository columns,
            TaskRepository tasks) {
        this.projects = projects;
        this.columns = columns;
        this.tasks = tasks;
    }

    @Transactional
    public List<TaskResponse> saveBatch(long userId, long projectId, String originalTitle,
            List<AiTaskItem> generated) {
        Context context = context(userId, projectId, ErrorCode.TASK_BATCH_SAVE_FAILED);
        String normalizedOriginalTitle = originalTitle.strip();
        List<Task> entities = new ArrayList<>();
        long next = context.nextSortOrder();
        for (AiTaskItem item : generated) {
            entities.add(new Task(projectId, context.columnId(), item.title(),
                    normalizedOriginalTitle + " - " + item.description(), item.priority(), next++));
        }
        try {
            return tasks.saveAllAndFlush(entities).stream().map(TaskResponse::from).toList();
        } catch (RuntimeException exception) {
            throw new DomainException(ErrorCode.TASK_BATCH_SAVE_FAILED, exception);
        }
    }

    @Transactional
    public List<TaskResponse> saveFallback(long userId, long projectId, String title,
            String description) {
        Context context = context(userId, projectId, ErrorCode.TASK_FALLBACK_SAVE_FAILED);
        try {
            Task saved = tasks.saveAndFlush(new Task(projectId, context.columnId(), title,
                    description, 1, context.nextSortOrder()));
            return List.of(TaskResponse.from(saved));
        } catch (RuntimeException exception) {
            throw new DomainException(ErrorCode.TASK_FALLBACK_SAVE_FAILED, exception);
        }
    }

    private Context context(long userId, long projectId, ApiCode internalFailure) {
        projects.findWithLockByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.PROJECT_NOT_FOUND));
        List<BoardColumn> ordered = columns.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
        if (ordered.isEmpty()) throw new DomainException(internalFailure);
        long columnId = ordered.getFirst().getId();
        long next = tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(projectId, columnId)
                .stream().mapToLong(Task::getSortOrder).max().orElse(0L) + 1L;
        return new Context(columnId, next);
    }

    private record Context(long columnId, long nextSortOrder) {}
}
