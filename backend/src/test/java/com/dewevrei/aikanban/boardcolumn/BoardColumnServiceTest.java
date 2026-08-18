package com.dewevrei.aikanban.boardcolumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class BoardColumnServiceTest {
    @Mock ProjectRepository projects;
    @Mock BoardColumnRepository columns;
    @Mock TaskRepository tasks;
    private BoardColumnService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new BoardColumnService(projects, columns, tasks);
        User owner = id(new User("g", "n", "a@b.com", "nick"), 1L);
        project = id(new Project(owner, "project", null), 10L);
    }

    @Test
    void 새_열은_Project_lock_후_현재_max_sortOrder_다음에_생성한다() {
        BoardColumn first = column(100L, "Todo", 3);
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(first));
        when(columns.saveAndFlush(any())).thenAnswer(call -> id(call.getArgument(0), 101L));

        ColumnResponse response = service.create(1L, 10L, new ColumnRequest(" Review "));

        assertThat(response.name()).isEqualTo("Review");
        assertThat(response.sortOrder()).isEqualTo(4);
    }

    @Test
    void 전체_reorder는_모든_id를_한번씩_요구하고_배열_첫_열을_sortOrder_1로_바꾼다() {
        BoardColumn todo = column(100L, "Todo", 1);
        BoardColumn done = column(101L, "Done", 2);
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(todo, done));

        List<ColumnResponse> result = service.reorder(1L, 10L,
                new ReorderColumnsRequest(List.of(101L, 100L)));

        assertThat(result).extracting(ColumnResponse::id).containsExactly(101L, 100L);
        assertThat(result).extracting(ColumnResponse::sortOrder).containsExactly(1, 2);
        assertThat(done.getSortOrder()).isEqualTo(1);
    }

    @Test
    void reorder의_null_empty_duplicate_missing_foreign_unknown을_모두_거부한다() {
        BoardColumn a = column(100L, "A", 1);
        BoardColumn b = column(101L, "B", 2);
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(a, b));

        List<List<Long>> invalid = java.util.Arrays.asList(null, List.of(), List.of(100L, 100L),
                List.of(100L), List.of(100L, 999L), List.of(100L, 101L, 999L));
        for (List<Long> ids : invalid) {
            assertCode(ApiCode.INVALID_COLUMN_ORDER,
                    () -> service.reorder(1L, 10L, new ReorderColumnsRequest(ids)));
        }
    }

    @Test
    void 삭제는_Project_write_lock을_먼저_잡고_열수를_재검사한_뒤_DB_cascade_삭제한다() {
        BoardColumn target = column(100L, "A", 1);
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.of(target));
        when(columns.countByProjectId(10L)).thenReturn(2L);

        service.delete(1L, 10L, 100L);

        InOrder order = inOrder(projects, columns);
        order.verify(projects).findWithLockByIdAndUserId(10L, 1L);
        order.verify(columns).findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L);
        order.verify(columns).countByProjectId(10L);
        order.verify(columns).delete(target);
        order.verify(columns).flush();
    }

    @Test
    void 동시삭제도_lock_획득후_마지막_열이면_변경없이_거부한다() {
        BoardColumn only = column(100L, "A", 1);
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.of(only));
        when(columns.countByProjectId(10L)).thenReturn(1L);

        assertCode(ApiCode.LAST_COLUMN_DELETE_FORBIDDEN, () -> service.delete(1L, 10L, 100L));

        verify(columns, never()).delete(any());
        verify(columns, never()).flush();
    }

    @Test
    void 타인_Project와_Column은_각각_존재를_숨긴_오류를_반환한다() {
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.PROJECT_NOT_FOUND,
                () -> service.create(1L, 10L, new ColumnRequest("A")));

        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.COLUMN_NOT_FOUND,
                () -> service.update(1L, 10L, 100L, new ColumnRequest("A")));
    }

    private BoardColumn column(long id, String name, int sortOrder) {
        return id(new BoardColumn(project, name, sortOrder), id);
    }

    private <T> T id(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private void assertCode(ApiCode code, Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
