package com.dewevrei.aikanban.project;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dewevrei.aikanban.auth.AuthenticatedUser;
import com.dewevrei.aikanban.boardcolumn.ColumnResponse;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.SuccessCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListData>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ok(SuccessCode.PROJECT_LISTED, new ProjectListData(service.list(user.userId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectData>> create(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ProjectRequest request) {
        ProjectService.CreatedProject created = service.create(user.userId(), request);
        return ResponseEntity.status(SuccessCode.PROJECT_CREATED.status()).body(ApiResponse.success(
                SuccessCode.PROJECT_CREATED, new ProjectData(created.project(), created.columns())));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectOnlyData>> get(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId) {
        return ok(SuccessCode.PROJECT_READ, new ProjectOnlyData(service.get(user.userId(), projectId)));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectOnlyData>> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @RequestBody ProjectRequest request) {
        return ok(SuccessCode.PROJECT_UPDATED,
                new ProjectOnlyData(service.update(user.userId(), projectId, request)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId) {
        service.delete(user.userId(), projectId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.PROJECT_DELETED, null));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(ApiCode code, T data) {
        return ResponseEntity.ok(ApiResponse.success(code, data));
    }

    public record ProjectListData(List<ProjectResponse> projects) {}
    public record ProjectData(ProjectResponse project, List<ColumnResponse> columns) {}
    public record ProjectOnlyData(ProjectResponse project) {}
}
