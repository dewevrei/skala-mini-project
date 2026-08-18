package com.dewevrei.aikanban.task;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dewevrei.aikanban.auth.AuthenticatedUser;
import com.dewevrei.aikanban.aitask.AiGenerateRequest;
import com.dewevrei.aikanban.aitask.AiTaskService;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class TaskController {
    private final TaskService service;
    private final AiTaskService aiTaskService;

    public TaskController(TaskService service, AiTaskService aiTaskService) {
        this.service = service;
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/tasks/ai-generate")
    public ResponseEntity<ApiResponse<TasksData>> generate(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long projectId,
            @RequestBody AiGenerateRequest request) {
        return ResponseEntity.status(ApiCode.TASKS_CREATED.status()).body(ApiResponse.success(
                ApiCode.TASKS_CREATED,
                new TasksData(aiTaskService.generate(user.userId(), projectId, request))));
    }

    @PostMapping("/columns/{columnId}/tasks")
    public ResponseEntity<ApiResponse<TaskData>> create(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long columnId,
            @RequestBody TaskContentRequest request) {
        return ResponseEntity.status(ApiCode.TASK_CREATED.status()).body(ApiResponse.success(
                ApiCode.TASK_CREATED, new TaskData(service.create(user.userId(), projectId, columnId, request))));
    }

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<TaskService.BoardData>> board(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long projectId) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.BOARD_READ,
                service.board(user.userId(), projectId)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<TaskService.BoardData>> items(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long projectId,
            @RequestParam(required = false) String title) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.ITEMS_READ,
                service.items(user.userId(), projectId, title)));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskData>> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long taskId) {
        return ok(ApiCode.TASK_READ, service.get(user.userId(), projectId, taskId));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskData>> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long taskId,
            @RequestBody TaskContentRequest request) {
        return ok(ApiCode.TASK_UPDATED, service.update(user.userId(), projectId, taskId, request));
    }

    @PatchMapping("/tasks/{taskId}/dates")
    public ResponseEntity<ApiResponse<TaskData>> updateDates(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long taskId,
            @RequestBody TaskDatesRequest request) {
        return ok(ApiCode.TASK_DATES_UPDATED,
                service.updateDates(user.userId(), projectId, taskId, request));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskService.MoveResult>> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long projectId,
            @PathVariable long taskId, @RequestBody TaskStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TASK_MOVED,
                service.changeStatus(user.userId(), projectId, taskId, request)));
    }

    @PatchMapping("/tasks/{taskId}/position")
    public ResponseEntity<ApiResponse<TaskService.MoveResult>> movePosition(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long projectId,
            @PathVariable long taskId, @RequestBody TaskPositionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TASK_MOVED,
                service.movePosition(user.userId(), projectId, taskId, request)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long taskId) {
        service.delete(user.userId(), projectId, taskId);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TASK_DELETED, null));
    }

    private ResponseEntity<ApiResponse<TaskData>> ok(ApiCode code, TaskResponse task) {
        return ResponseEntity.ok(ApiResponse.success(code, new TaskData(task)));
    }

    public record TaskData(TaskResponse task) {}
    public record TasksData(java.util.List<TaskResponse> tasks) {}
}
