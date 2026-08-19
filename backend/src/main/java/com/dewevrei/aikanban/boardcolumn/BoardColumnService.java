package com.dewevrei.aikanban.boardcolumn;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ErrorCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;
import com.dewevrei.aikanban.task.ColumnTaskCount;

@Service
@Transactional(readOnly = true)
public class BoardColumnService {
    private final ProjectRepository projectRepository;
    private final BoardColumnRepository columnRepository;
    private final TaskRepository taskRepository;

    public BoardColumnService(ProjectRepository projectRepository,
            BoardColumnRepository columnRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public ColumnResponse create(long userId, long projectId, ColumnRequest request) {
        validate(request);
        String name = UserInputValidator.required(request.name(), 50, ErrorCode.INVALID_COLUMN_NAME);
        Project project = lockedOwnedProject(userId, projectId);
        if (columnRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new DomainException(ErrorCode.DUPLICATE_COLUMN_NAME);
        }
        List<BoardColumn> current = ordered(projectId);
        int next = current.stream().mapToInt(BoardColumn::getSortOrder).max().orElse(0) + 1;
        try {
            BoardColumn saved = columnRepository.saveAndFlush(new BoardColumn(project, name, next));
            return ColumnResponse.from(saved, 0);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ErrorCode.DUPLICATE_COLUMN_NAME, exception);
        }
    }

    @Transactional
    public ColumnResponse update(long userId, long projectId, long columnId, ColumnRequest request) {
        validate(request);
        String name = UserInputValidator.required(request.name(), 50, ErrorCode.INVALID_COLUMN_NAME);
        ownedProject(userId, projectId);
        BoardColumn column = ownedColumn(userId, projectId, columnId);
        if (columnRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name, columnId)) {
            throw new DomainException(ErrorCode.DUPLICATE_COLUMN_NAME);
        }
        column.rename(name);
        try {
            column = columnRepository.saveAndFlush(column);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ErrorCode.DUPLICATE_COLUMN_NAME, exception);
        }
        return response(column);
    }

    @Transactional
    public List<ColumnResponse> reorder(long userId, long projectId, ReorderColumnsRequest request) {
        validate(request);
        lockedOwnedProject(userId, projectId);
        List<BoardColumn> columns = ordered(projectId);
        List<Long> ids = request.orderedColumnIds();
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null)
                || ids.size() != columns.size()) {
            throw new DomainException(ErrorCode.INVALID_COLUMN_ORDER);
        }
        Set<Long> requested = new HashSet<>(ids);
        Set<Long> current = columns.stream().map(BoardColumn::getId).collect(Collectors.toSet());
        if (requested.size() != ids.size() || !requested.equals(current)) {
            throw new DomainException(ErrorCode.INVALID_COLUMN_ORDER);
        }
        var byId = columns.stream().collect(Collectors.toMap(BoardColumn::getId, c -> c));
        for (int index = 0; index < ids.size(); index++) {
            byId.get(ids.get(index)).reorder(index + 1);
        }
        columnRepository.saveAllAndFlush(columns);
        Map<Long, Long> taskCounts = taskRepository.countAllByColumnId(projectId).stream()
                .collect(Collectors.toMap(ColumnTaskCount::columnId, ColumnTaskCount::taskCount));
        return ids.stream().map(byId::get)
                .map(column -> ColumnResponse.from(column, taskCounts.getOrDefault(column.getId(), 0L)))
                .toList();
    }

    @Transactional
    public void delete(long userId, long projectId, long columnId) {
        lockedOwnedProject(userId, projectId);
        BoardColumn column = ownedColumn(userId, projectId, columnId);
        if (columnRepository.countByProjectId(projectId) <= 1) {
            throw new DomainException(ErrorCode.LAST_COLUMN_DELETE_FORBIDDEN);
        }
        try {
            columnRepository.delete(column);
            columnRepository.flush();
        } catch (RuntimeException exception) {
            throw new DomainException(ErrorCode.COLUMN_DELETE_FAILED, exception);
        }
    }

    private Project ownedProject(long userId, long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Project lockedOwnedProject(long userId, long projectId) {
        return projectRepository.findWithLockByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private BoardColumn ownedColumn(long userId, long projectId, long columnId) {
        return columnRepository.findByIdAndProjectIdAndProjectUserId(columnId, projectId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.COLUMN_NOT_FOUND));
    }

    private List<BoardColumn> ordered(long projectId) {
        return columnRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
    }

    private ColumnResponse response(BoardColumn column) {
        return ColumnResponse.from(column, taskRepository.countByColumnId(column.getId()));
    }

    private void validate(ColumnRequest request) {
        if (request == null || request.hasUnknownField()) throw new DomainException(ErrorCode.INVALID_REQUEST);
        if (request.hasReadOnlyField()) throw new DomainException(ErrorCode.READ_ONLY_FIELD);
    }

    private void validate(ReorderColumnsRequest request) {
        if (request == null || request.hasUnknownField()) throw new DomainException(ErrorCode.INVALID_REQUEST);
        if (request.hasReadOnlyField()) throw new DomainException(ErrorCode.READ_ONLY_FIELD);
    }
}
