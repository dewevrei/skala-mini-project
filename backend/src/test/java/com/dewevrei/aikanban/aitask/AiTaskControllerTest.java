package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dewevrei.aikanban.auth.AuthenticatedUser;
import com.dewevrei.aikanban.task.TaskController;
import com.dewevrei.aikanban.task.TaskResponse;
import com.dewevrei.aikanban.task.TaskService;

@ExtendWith(MockitoExtension.class)
class AiTaskControllerTest {
    @Mock TaskService taskService;
    @Mock AiTaskService aiTaskService;

    @Test
    void 성공은_201_TASKS_CREATED와_exact_tasks_data를_반환한다() {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 8, 18, 12, 0, 0, 0,
                ZoneOffset.ofHours(9));
        TaskResponse task = new TaskResponse(1L, 10L, 100L, "task", "original - detail",
                null, null, 2, 1L, timestamp, timestamp);
        when(aiTaskService.generate(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(task));
        TaskController controller = new TaskController(taskService, aiTaskService);

        var response = controller.generate(() -> 1L, 10L,
                new AiGenerateRequest("title", "description"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().code()).isEqualTo("TASKS_CREATED");
        assertThat(response.getBody().data().tasks()).containsExactly(task);
        assertThat(response.getBody().data().tasks().getFirst().createdAt().getOffset())
                .isEqualTo(ZoneOffset.ofHours(9));
    }
}
