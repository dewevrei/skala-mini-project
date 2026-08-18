package com.dewevrei.aikanban.project;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.boardcolumn.ColumnResponse;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;
import com.dewevrei.aikanban.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private static final List<String> DEFAULT_COLUMNS = List.of("Todo", "In Progress", "Done");

    private final ProjectRepository projectRepository;
    private final BoardColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, BoardColumnRepository columnRepository,
            TaskRepository taskRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<ProjectResponse> list(long userId) {
        return projectRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(ProjectResponse::from).toList();
    }

    public ProjectResponse get(long userId, long projectId) {
        return ProjectResponse.from(owned(userId, projectId));
    }

    @Transactional
    public CreatedProject create(long userId, ProjectRequest request) {
        validateEnvelope(request);
        String name = UserInputValidator.required(request.name(), 100, ApiCode.INVALID_PROJECT_NAME);
        String description = UserInputValidator.optional(request.description(), 2000,
                ApiCode.INVALID_PROJECT_DESCRIPTION);
        if (projectRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new DomainException(ApiCode.DUPLICATE_PROJECT_NAME);
        }
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ApiCode.AUTHENTICATION_REQUIRED));
        Project project;
        try {
            project = projectRepository.saveAndFlush(new Project(owner, name, description));
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ApiCode.DUPLICATE_PROJECT_NAME, exception);
        } catch (RuntimeException exception) {
            throw new DomainException(ApiCode.PROJECT_CREATE_FAILED, exception);
        }

        List<BoardColumn> columns = DEFAULT_COLUMNS.stream()
                .map(column -> new BoardColumn(project, column, DEFAULT_COLUMNS.indexOf(column) + 1))
                .toList();
        try {
            columns = columnRepository.saveAllAndFlush(columns);
        } catch (RuntimeException exception) {
            throw new DomainException(ApiCode.PROJECT_CREATE_FAILED, exception);
        }
        return new CreatedProject(ProjectResponse.from(project),
                columns.stream().map(column -> ColumnResponse.from(column, 0)).toList());
    }

    @Transactional
    public ProjectResponse update(long userId, long projectId, ProjectRequest request) {
        validateEnvelope(request);
        String name = UserInputValidator.required(request.name(), 100, ApiCode.INVALID_PROJECT_NAME);
        String description = UserInputValidator.optional(request.description(), 2000,
                ApiCode.INVALID_PROJECT_DESCRIPTION);
        Project project = owned(userId, projectId);
        if (projectRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, projectId)) {
            throw new DomainException(ApiCode.DUPLICATE_PROJECT_NAME);
        }
        project.update(name, description);
        try {
            return ProjectResponse.from(projectRepository.saveAndFlush(project));
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(ApiCode.DUPLICATE_PROJECT_NAME, exception);
        }
    }

    @Transactional
    public void delete(long userId, long projectId) {
        Project project = owned(userId, projectId);
        try {
            projectRepository.delete(project);
            projectRepository.flush();
        } catch (RuntimeException exception) {
            throw new DomainException(ApiCode.PROJECT_DELETE_FAILED, exception);
        }
    }

    private Project owned(long userId, long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ApiCode.PROJECT_NOT_FOUND));
    }

    private void validateEnvelope(ProjectRequest request) {
        if (request == null || request.hasUnknownField()) throw new DomainException(ApiCode.INVALID_REQUEST);
        if (request.hasReadOnlyField()) throw new DomainException(ApiCode.READ_ONLY_FIELD);
    }

    public record CreatedProject(ProjectResponse project, List<ColumnResponse> columns) {}
}
