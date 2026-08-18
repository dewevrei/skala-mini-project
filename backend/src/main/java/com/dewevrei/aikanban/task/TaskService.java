package com.dewevrei.aikanban.task;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.boardcolumn.ColumnResponse;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.project.ProjectResponse;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final ProjectRepository projectRepository;
    private final BoardColumnRepository columnRepository;
    private final TaskRepository taskRepository;

    public TaskService(ProjectRepository projectRepository, BoardColumnRepository columnRepository,
            TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(long userId, long projectId, long columnId, TaskContentRequest request) {
        validateContentEnvelope(request);
        String title = UserInputValidator.required(request.title(), 200, ApiCode.INVALID_TASK_TITLE);
        String description = UserInputValidator.optional(request.description(), 5000,
                ApiCode.INVALID_TASK_DESCRIPTION);
        lockedOwnedProject(userId, projectId);
        ownedColumn(userId, projectId, columnId);
        List<Task> current = orderedTasks(projectId, columnId);
        long next = current.stream().mapToLong(Task::getSortOrder).max().orElse(0L) + 1L;
        return TaskResponse.from(taskRepository.saveAndFlush(
                new Task(projectId, columnId, title, description, 1, next)));
    }

    public TaskResponse get(long userId, long projectId, long taskId) {
        ownedProject(userId, projectId);
        return TaskResponse.from(ownedTask(userId, projectId, taskId));
    }

    @Transactional
    public TaskResponse update(long userId, long projectId, long taskId, TaskContentRequest request) {
        validateContentEnvelope(request);
        String title = UserInputValidator.required(request.title(), 200, ApiCode.INVALID_TASK_TITLE);
        String description = UserInputValidator.optional(request.description(), 5000,
                ApiCode.INVALID_TASK_DESCRIPTION);
        ownedProject(userId, projectId);
        Task task = ownedTask(userId, projectId, taskId);
        task.updateContent(title, description);
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse updateDates(long userId, long projectId, long taskId, TaskDatesRequest request) {
        if (request == null || request.hasUnknownField() || !request.hasBothKeys()) {
            throw new DomainException(ApiCode.INVALID_TASK_DATE);
        }
        LocalDate startDate = parseDate(request.startDate());
        LocalDate endDate = parseDate(request.endDate());
        ownedProject(userId, projectId);
        Task task = ownedTask(userId, projectId, taskId);
        task.updateDates(startDate, endDate);
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(long userId, long projectId, long taskId) {
        lockedOwnedProject(userId, projectId);
        Task task = ownedTask(userId, projectId, taskId);
        List<Task> remaining = new ArrayList<>(orderedTasks(projectId, task.getColumnId()));
        remaining.removeIf(candidate -> candidate.getId().equals(taskId));
        normalize(remaining, task.getColumnId());
        try {
            taskRepository.delete(task);
            taskRepository.saveAll(remaining);
            taskRepository.flush();
        } catch (RuntimeException exception) {
            throw new DomainException(ApiCode.TASK_DELETE_FAILED, exception);
        }
    }

    public BoardData board(long userId, long projectId) {
        Project project = ownedProject(userId, projectId);
        List<BoardColumn> columns = orderedColumns(projectId);
        List<Task> tasks = taskRepository.findAllForItems(projectId);
        return new BoardData(ProjectResponse.from(project), groups(columns, tasks, false));
    }

    public BoardData items(long userId, long projectId, String titleQuery) {
        Project project = ownedProject(userId, projectId);
        String query = UserInputValidator.optional(titleQuery, 200, ApiCode.INVALID_SEARCH_QUERY);
        List<BoardColumn> columns = orderedColumns(projectId);
        List<Task> tasks = query == null || query.isEmpty()
                ? taskRepository.findAllForItems(projectId)
                : taskRepository.searchAllForItems(projectId, escapeLikePattern(query));
        return new BoardData(ProjectResponse.from(project), groups(columns, tasks, true));
    }

    @Transactional
    public MoveResult changeStatus(long userId, long projectId, long taskId, TaskStatusRequest request) {
        if (request == null || request.hasUnknownField() || request.targetColumnId() == null) {
            throw new DomainException(ApiCode.INVALID_TASK_MOVE);
        }
        return move(userId, projectId, taskId, request.targetColumnId(), null);
    }

    @Transactional
    public MoveResult movePosition(long userId, long projectId, long taskId, TaskPositionRequest request) {
        if (request == null || request.hasUnknownField() || request.targetColumnId() == null
                || (request.beforeTaskId() != null && request.beforeTaskId().equals(taskId))) {
            throw new DomainException(ApiCode.INVALID_TASK_MOVE);
        }
        return move(userId, projectId, taskId, request.targetColumnId(), request.beforeTaskId());
    }

    private MoveResult move(long userId, long projectId, long taskId, long targetColumnId,
            Long beforeTaskId) {
        lockedOwnedProject(userId, projectId);
        Task task = ownedTask(userId, projectId, taskId);
        BoardColumn targetColumn = ownedColumn(userId, projectId, targetColumnId);
        long sourceColumnId = task.getColumnId();
        BoardColumn sourceColumn = sourceColumnId == targetColumnId
                ? targetColumn
                : ownedColumn(userId, projectId, sourceColumnId);

        List<Task> source = new ArrayList<>(orderedTasks(projectId, sourceColumnId));
        List<Task> target = sourceColumnId == targetColumnId
                ? source
                : new ArrayList<>(orderedTasks(projectId, targetColumnId));
        if (!source.removeIf(candidate -> candidate.getId().equals(taskId))) {
            throw new DomainException(ApiCode.INVALID_TASK_MOVE);
        }

        int insertionIndex = target.size();
        if (beforeTaskId != null) {
            insertionIndex = indexOf(target, beforeTaskId);
            if (insertionIndex < 0) throw new DomainException(ApiCode.INVALID_TASK_MOVE);
        }
        target.add(insertionIndex, task);

        normalize(source, sourceColumnId);
        if (source != target) normalize(target, targetColumnId);

        List<Task> changed = source == target
                ? source
                : java.util.stream.Stream.concat(source.stream(), target.stream()).toList();
        taskRepository.saveAllAndFlush(changed);

        List<ColumnGroupResponse> affected = new ArrayList<>();
        affected.add(group(sourceColumn, source));
        if (source != target) affected.add(group(targetColumn, target));
        affected.sort(Comparator.comparingInt((ColumnGroupResponse group) -> group.column().sortOrder())
                .thenComparing(group -> group.column().id()));
        return new MoveResult(TaskResponse.from(task), affected);
    }

    private List<ColumnGroupResponse> groups(List<BoardColumn> columns, List<Task> tasks,
            boolean countAllTasks) {
        Map<Long, List<Task>> byColumn = tasks.stream().collect(Collectors.groupingBy(Task::getColumnId,
                LinkedHashMap::new, Collectors.toList()));
        return columns.stream().map(column -> {
            List<Task> columnTasks = byColumn.getOrDefault(column.getId(), List.of());
            long taskCount = countAllTasks ? taskRepository.countByColumnId(column.getId()) : columnTasks.size();
            return new ColumnGroupResponse(ColumnResponse.from(column, taskCount),
                    columnTasks.stream().map(TaskResponse::from).toList());
        }).toList();
    }

    private ColumnGroupResponse group(BoardColumn column, List<Task> tasks) {
        return new ColumnGroupResponse(ColumnResponse.from(column, tasks.size()),
                tasks.stream().map(TaskResponse::from).toList());
    }

    private void normalize(List<Task> tasks, long columnId) {
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).moveTo(tasks.get(index).getProjectId(), columnId, index + 1L);
        }
    }

    private int indexOf(List<Task> tasks, long taskId) {
        for (int index = 0; index < tasks.size(); index++) {
            if (tasks.get(index).getId().equals(taskId)) return index;
        }
        return -1;
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DomainException(ApiCode.INVALID_TASK_DATE);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DomainException(ApiCode.INVALID_TASK_DATE, exception);
        }
    }

    private String escapeLikePattern(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private void validateContentEnvelope(TaskContentRequest request) {
        if (request == null || request.hasUnknownField()) throw new DomainException(ApiCode.INVALID_REQUEST);
        if (request.hasReadOnlyField()) throw new DomainException(ApiCode.READ_ONLY_FIELD);
    }

    private Project ownedProject(long userId, long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ApiCode.PROJECT_NOT_FOUND));
    }

    private Project lockedOwnedProject(long userId, long projectId) {
        return projectRepository.findWithLockByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ApiCode.PROJECT_NOT_FOUND));
    }

    private BoardColumn ownedColumn(long userId, long projectId, long columnId) {
        return columnRepository.findByIdAndProjectIdAndProjectUserId(columnId, projectId, userId)
                .orElseThrow(() -> new DomainException(ApiCode.COLUMN_NOT_FOUND));
    }

    private Task ownedTask(long userId, long projectId, long taskId) {
        return taskRepository.findOwned(taskId, projectId, userId)
                .orElseThrow(() -> new DomainException(ApiCode.TASK_NOT_FOUND));
    }

    private List<BoardColumn> orderedColumns(long projectId) {
        return columnRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
    }

    private List<Task> orderedTasks(long projectId, long columnId) {
        return taskRepository.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(projectId, columnId);
    }

    public record BoardData(ProjectResponse project, List<ColumnGroupResponse> columnGroups) {}
    public record MoveResult(TaskResponse task, List<ColumnGroupResponse> affectedColumnGroups) {}
}
