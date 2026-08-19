package com.dewevrei.aikanban.aitask;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ErrorCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.common.validation.UserInputValidator;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.task.TaskResponse;

@Service
public class AiTaskService {
    private final ProjectRepository projects;
    private final AiTaskGenerator generator;
    private final AiTaskPersistence persistence;

    public AiTaskService(ProjectRepository projects, AiTaskGenerator generator,
            AiTaskPersistence persistence) {
        this.projects = projects;
        this.generator = generator;
        this.persistence = persistence;
    }

    public List<TaskResponse> generate(long userId, long projectId, AiGenerateRequest request) {
        if (request == null || request.hasUnknownField()) throw new DomainException(ErrorCode.INVALID_REQUEST);
        String title = required(request.title(), 200, ErrorCode.INVALID_TASK_TITLE);
        String description = required(request.description(), 5000, ErrorCode.INVALID_AI_DESCRIPTION);

        projects.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.PROJECT_NOT_FOUND));

        try {
            List<AiTaskItem> generated;
            try {
                generated = generator.generate(title, description);
            } catch (InvalidAiResponseException firstInvalid) {
                generated = generator.generate(title, description);
            }
            return persistence.saveBatch(userId, projectId, title, generated);
        } catch (InvalidAiResponseException | AiGenerationException aiFailure) {
            return persistence.saveFallback(userId, projectId, title, description);
        }
    }

    private String required(String value, int max, ApiCode code) {
        try {
            return UserInputValidator.required(value, max);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(code, exception);
        }
    }
}
