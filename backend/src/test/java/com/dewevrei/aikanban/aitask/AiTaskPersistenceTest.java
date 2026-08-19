package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ErrorCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class AiTaskPersistenceTest {
    @Mock ProjectRepository projects;
    @Mock BoardColumnRepository columns;
    @Mock TaskRepository tasks;
    private AiTaskPersistence persistence;

    @BeforeEach
    void setUp() { persistence = new AiTaskPersistence(projects, columns, tasks); }

    @Test
    void 현재_첫_열의_맨아래부터_AI_순서대로_연속저장한다() {
        BoardColumn second = column(102L, 2);
        BoardColumn first = column(101L, 1);
        Task existing = new Task(10L, 101L, "old", null, 1, 7L);
        context(List.of(first, second), List.of(existing));
        when(tasks.saveAllAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        persistence.saveBatch(1L, 10L, " original ", List.of(
                new AiTaskItem("first", "one", 2), new AiTaskItem("second", "two", 5)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Task>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(tasks).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue()).extracting(Task::getTitle).containsExactly("first", "second");
        assertThat(captor.getValue()).extracting(Task::getDescription)
                .containsExactly("original - one", "original - two");
        assertThat(captor.getValue()).extracting(Task::getColumnId).containsOnly(101L);
        assertThat(captor.getValue()).extracting(Task::getSortOrder).containsExactly(8L, 9L);
        assertThat(captor.getValue()).extracting(Task::getPriority).containsExactly(2, 5);
        assertThat(captor.getValue()).allSatisfy(task -> {
            assertThat(task.getStartDate()).isNull();
            assertThat(task.getEndDate()).isNull();
        });
    }

    @Test
    void fallback은_첫열_맨아래에_원본과_priority1을_저장한다() {
        context(List.of(column(101L, 1)), List.of(new Task(10L, 101L, "old", null, 1, 3L)));
        when(tasks.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        var result = persistence.saveFallback(1L, 10L, "title", "description");

        assertThat(result.getFirst().title()).isEqualTo("title");
        assertThat(result.getFirst().description()).isEqualTo("description");
        assertThat(result.getFirst().priority()).isEqualTo(1);
        assertThat(result.getFirst().sortOrder()).isEqualTo(4L);
        assertThat(result.getFirst().startDate()).isNull();
        assertThat(result.getFirst().endDate()).isNull();
    }

    @Test
    void batch_DB실패와_fallback_DB실패를_각각_정확히_매핑한다() {
        context(List.of(column(101L, 1)), List.of());
        when(tasks.saveAllAndFlush(any())).thenThrow(new RuntimeException("db"));
        assertCode(ErrorCode.TASK_BATCH_SAVE_FAILED, () -> persistence.saveBatch(1L, 10L,
                "title", List.of(new AiTaskItem("one", "detail", 1))));

        when(tasks.saveAndFlush(any())).thenThrow(new RuntimeException("db"));
        assertCode(ErrorCode.TASK_FALLBACK_SAVE_FAILED,
                () -> persistence.saveFallback(1L, 10L, "title", "description"));
    }

    @Test
    void 저장시점에_소유권을_다시_확인하고_트랜잭션_경계를_저장에만_둔다() throws Exception {
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertCode(ErrorCode.PROJECT_NOT_FOUND, () -> persistence.saveFallback(1L, 10L, "t", "d"));

        Method generate = AiTaskService.class.getMethod("generate", long.class, long.class,
                AiGenerateRequest.class);
        Method saveBatch = AiTaskPersistence.class.getMethod("saveBatch", long.class, long.class,
                String.class, List.class);
        Method saveFallback = AiTaskPersistence.class.getMethod("saveFallback", long.class, long.class,
                String.class, String.class);
        assertThat(generate.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(saveBatch.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(saveFallback.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void context(List<BoardColumn> orderedColumns, List<Task> orderedTasks) {
        when(projects.findWithLockByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(orderedColumns);
        if (!orderedColumns.isEmpty()) {
            when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L,
                    orderedColumns.getFirst().getId())).thenReturn(orderedTasks);
        }
    }

    private BoardColumn column(long id, int order) {
        BoardColumn column = org.mockito.Mockito.mock(BoardColumn.class);
        org.mockito.Mockito.lenient().when(column.getId()).thenReturn(id);
        return column;
    }

    private void assertCode(ApiCode code, Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
