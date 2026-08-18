package com.dewevrei.aikanban.boardcolumn;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dewevrei.aikanban.auth.AuthenticatedUser;
import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/columns")
public class BoardColumnController {
    private final BoardColumnService service;

    public BoardColumnController(BoardColumnService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ColumnData>> create(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @RequestBody ColumnRequest request) {
        return ResponseEntity.status(ApiCode.COLUMN_CREATED.status()).body(ApiResponse.success(
                ApiCode.COLUMN_CREATED, new ColumnData(service.create(user.userId(), projectId, request))));
    }

    @PatchMapping("/{columnId}")
    public ResponseEntity<ApiResponse<ColumnData>> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long columnId, @RequestBody ColumnRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.COLUMN_UPDATED,
                new ColumnData(service.update(user.userId(), projectId, columnId, request))));
    }

    @PutMapping("/order")
    public ResponseEntity<ApiResponse<ColumnsData>> reorder(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @RequestBody ReorderColumnsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ApiCode.COLUMNS_REORDERED,
                new ColumnsData(service.reorder(user.userId(), projectId, request))));
    }

    @DeleteMapping("/{columnId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long projectId, @PathVariable long columnId) {
        service.delete(user.userId(), projectId, columnId);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.COLUMN_DELETED, null));
    }

    public record ColumnData(ColumnResponse column) {}
    public record ColumnsData(List<ColumnResponse> columns) {}
}
