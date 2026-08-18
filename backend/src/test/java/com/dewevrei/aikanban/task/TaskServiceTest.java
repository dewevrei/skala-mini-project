package com.dewevrei.aikanban.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock ProjectRepository projects;
    @Mock BoardColumnRepository columns;
    @Mock TaskRepository tasks;
    private TaskService service;
    private Project project;
    private BoardColumn todo;
    private BoardColumn done;

    @BeforeEach
    void setUp() {
        service = new TaskService(projects, columns, tasks);
        User owner = id(new User("google", "name", "a@b.com", "nick"), 1L);
        project = id(new Project(owner, "project", null), 10L);
        todo = id(new BoardColumn(project, "Todo", 1), 100L);
        done = id(new BoardColumn(project, "Done", 2), 101L);
    }

    @Test
    void 일반_생성은_입력을_trim하고_기본값과_열_맨아래_순서를_설정한다() {
        Task old = task(1L, 100L, 4L, "old");
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.of(todo));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 100L))
                .thenReturn(List.of(old));
        when(tasks.saveAndFlush(any())).thenAnswer(call -> id(call.getArgument(0), 2L));

        TaskResponse result = service.create(1L, 10L, 100L,
                new TaskContentRequest("  new task  ", "  details  "));

        assertThat(result.title()).isEqualTo("new task");
        assertThat(result.description()).isEqualTo("details");
        assertThat(result.priority()).isEqualTo(1);
        assertThat(result.sortOrder()).isEqualTo(5L);
        assertThat(result.startDate()).isNull();
        assertThat(result.endDate()).isNull();
    }

    @Test
    void 생성은_Project와_Column_소유권_및_경로_일치를_검사한다() {
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.PROJECT_NOT_FOUND, () -> service.create(1L, 10L, 100L,
                new TaskContentRequest("task", null)));

        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.COLUMN_NOT_FOUND, () -> service.create(1L, 10L, 100L,
                new TaskContentRequest("task", null)));
    }

    @Test
    void 생성과_수정은_읽기전용_필드를_조용히_무시하지_않는다() {
        TaskContentRequest create = new TaskContentRequest("task", null);
        create.captureUnknown("priority", 5);
        assertCode(ApiCode.READ_ONLY_FIELD, () -> service.create(1L, 10L, 100L, create));

        TaskContentRequest update = new TaskContentRequest("task", null);
        update.captureUnknown("startDate", "2026-08-18");
        assertCode(ApiCode.READ_ONLY_FIELD, () -> service.update(1L, 10L, 1L, update));
        verify(projects, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void 제목_설명_수정은_소유_Task만_변경하고_서버_정규값을_반환한다() {
        Task target = task(1L, 100L, 1L, "before");
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(1L, 10L, 1L)).thenReturn(Optional.of(target));
        when(tasks.saveAndFlush(target)).thenReturn(target);

        TaskResponse result = service.update(1L, 10L, 1L,
                new TaskContentRequest(" after ", " changed "));

        assertThat(result.title()).isEqualTo("after");
        assertThat(result.description()).isEqualTo("changed");
        assertThat(result.columnId()).isEqualTo(100L);
        assertThat(result.priority()).isEqualTo(1);
    }

    @Test
    void 타인이나_다른_Project의_Task는_TASK_NOT_FOUND로_숨긴다() {
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(1L, 10L, 1L)).thenReturn(Optional.empty());

        assertCode(ApiCode.TASK_NOT_FOUND, () -> service.get(1L, 10L, 1L));
    }

    @Test
    void 날짜는_설정_해제_역순을_모두_허용한다() {
        Task target = task(1L, 100L, 1L, "task");
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(1L, 10L, 1L)).thenReturn(Optional.of(target));
        when(tasks.saveAndFlush(target)).thenReturn(target);

        TaskResponse reversed = service.updateDates(1L, 10L, 1L,
                new TaskDatesRequest("2026-08-20", "2026-08-18"));
        assertThat(reversed.startDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(reversed.endDate()).isEqualTo(LocalDate.of(2026, 8, 18));

        TaskResponse cleared = service.updateDates(1L, 10L, 1L,
                new TaskDatesRequest(null, null));
        assertThat(cleared.startDate()).isNull();
        assertThat(cleared.endDate()).isNull();
    }

    @Test
    void 날짜의_두_key_누락과_잘못된_달력날짜를_거부한다() {
        assertCode(ApiCode.INVALID_TASK_DATE,
                () -> service.updateDates(1L, 10L, 1L, new TaskDatesRequest()));
        assertCode(ApiCode.INVALID_TASK_DATE,
                () -> service.updateDates(1L, 10L, 1L,
                        new TaskDatesRequest("2026-02-30", null)));
    }

    @Test
    void Board는_빈_열을_포함하고_열과_Task의_저장순서를_그대로_그룹화한다() {
        Task second = task(2L, 100L, 2L, "second");
        Task first = task(1L, 100L, 1L, "first");
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(todo, done));
        when(tasks.findAllForItems(10L)).thenReturn(List.of(first, second));

        TaskService.BoardData result = service.board(1L, 10L);

        assertThat(result.columnGroups()).extracting(group -> group.column().id())
                .containsExactly(100L, 101L);
        assertThat(result.columnGroups().get(0).tasks()).extracting(TaskResponse::id)
                .containsExactly(1L, 2L);
        assertThat(result.columnGroups().get(0).column().taskCount()).isEqualTo(2L);
        assertThat(result.columnGroups().get(1).tasks()).isEmpty();
    }

    @Test
    void Items_검색은_trim된_검색어를_사용하고_빈_그룹과_전체_taskCount를_유지한다() {
        Task match = task(1L, 100L, 1L, "Alpha");
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(todo, done));
        when(tasks.searchAllForItems(10L, "aLpHa")).thenReturn(List.of(match));
        when(tasks.countByColumnId(100L)).thenReturn(5L);
        when(tasks.countByColumnId(101L)).thenReturn(3L);

        TaskService.BoardData result = service.items(1L, 10L, "  aLpHa  ");

        verify(tasks).searchAllForItems(10L, "aLpHa");
        assertThat(result.columnGroups()).hasSize(2);
        assertThat(result.columnGroups().get(0).column().taskCount()).isEqualTo(5L);
        assertThat(result.columnGroups().get(1).column().taskCount()).isEqualTo(3L);
        assertThat(result.columnGroups().get(1).tasks()).isEmpty();
    }

    @Test
    void Items의_null과_blank_검색은_전체조회하고_200자를_넘으면_거부한다() {
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(columns.findAllByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());
        when(tasks.findAllForItems(10L)).thenReturn(List.of());

        service.items(1L, 10L, null);
        service.items(1L, 10L, "   ");
        verify(tasks, org.mockito.Mockito.times(2)).findAllForItems(10L);

        assertCode(ApiCode.INVALID_SEARCH_QUERY,
                () -> service.items(1L, 10L, "x".repeat(201)));
    }

    @Test
    void 같은_열에서_before_Task_바로_앞으로_이동하고_한_group만_반환한다() {
        Task one = task(1L, 100L, 1L, "one");
        Task two = task(2L, 100L, 2L, "two");
        Task three = task(3L, 100L, 3L, "three");
        stubMove(one, todo, List.of(one, two, three));

        TaskService.MoveResult result = service.movePosition(1L, 10L, 1L,
                new TaskPositionRequest(100L, 3L));

        assertThat(result.affectedColumnGroups()).hasSize(1);
        assertThat(result.affectedColumnGroups().getFirst().tasks()).extracting(TaskResponse::id)
                .containsExactly(2L, 1L, 3L);
        assertThat(List.of(two.getSortOrder(), one.getSortOrder(), three.getSortOrder()))
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void 다른_열로_이동하면_원본을_재정렬하고_대상_before_앞에_넣어_열순서로_두_group을_반환한다() {
        Task moving = task(1L, 100L, 2L, "moving");
        Task sourceFirst = task(2L, 100L, 1L, "source");
        Task targetFirst = task(3L, 101L, 1L, "target");
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(1L, 10L, 1L)).thenReturn(Optional.of(moving));
        when(columns.findByIdAndProjectIdAndProjectUserId(101L, 10L, 1L)).thenReturn(Optional.of(done));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.of(todo));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 100L))
                .thenReturn(List.of(sourceFirst, moving));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 101L))
                .thenReturn(List.of(targetFirst));

        TaskService.MoveResult result = service.movePosition(1L, 10L, 1L,
                new TaskPositionRequest(101L, 3L));

        assertThat(result.task().columnId()).isEqualTo(101L);
        assertThat(result.affectedColumnGroups()).extracting(group -> group.column().id())
                .containsExactly(100L, 101L);
        assertThat(result.affectedColumnGroups().get(0).tasks()).extracting(TaskResponse::id)
                .containsExactly(2L);
        assertThat(result.affectedColumnGroups().get(1).tasks()).extracting(TaskResponse::id)
                .containsExactly(1L, 3L);
    }

    @Test
    void Status_변경과_before_null은_대상_맨아래로_이동한다() {
        Task moving = task(1L, 100L, 1L, "moving");
        Task target = task(2L, 101L, 1L, "target");
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(1L, 10L, 1L)).thenReturn(Optional.of(moving));
        when(columns.findByIdAndProjectIdAndProjectUserId(101L, 10L, 1L)).thenReturn(Optional.of(done));
        when(columns.findByIdAndProjectIdAndProjectUserId(100L, 10L, 1L)).thenReturn(Optional.of(todo));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 100L))
                .thenReturn(List.of(moving));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 101L))
                .thenReturn(List.of(target));

        TaskService.MoveResult result = service.changeStatus(1L, 10L, 1L,
                new TaskStatusRequest(101L));

        assertThat(result.affectedColumnGroups().get(1).tasks()).extracting(TaskResponse::id)
                .containsExactly(2L, 1L);
        assertThat(result.task().sortOrder()).isEqualTo(2L);
    }

    @Test
    void 이동의_self_before_대상열에_없는_before와_다른_Project_열을_거부한다() {
        assertCode(ApiCode.INVALID_TASK_MOVE, () -> service.movePosition(1L, 10L, 1L,
                new TaskPositionRequest(100L, 1L)));

        Task moving = task(1L, 100L, 1L, "moving");
        stubMove(moving, todo, List.of(moving));
        assertCode(ApiCode.INVALID_TASK_MOVE, () -> service.movePosition(1L, 10L, 1L,
                new TaskPositionRequest(100L, 999L)));

        when(columns.findByIdAndProjectIdAndProjectUserId(999L, 10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.COLUMN_NOT_FOUND, () -> service.changeStatus(1L, 10L, 1L,
                new TaskStatusRequest(999L)));
    }

    @Test
    void 삭제은_Task를_완전삭제하고_남은_열_순서를_정규화하며_DB실패를_매핑한다() {
        Task first = task(1L, 100L, 1L, "first");
        Task deleted = task(2L, 100L, 2L, "deleted");
        Task third = task(3L, 100L, 3L, "third");
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(2L, 10L, 1L)).thenReturn(Optional.of(deleted));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, 100L))
                .thenReturn(List.of(first, deleted, third));

        service.delete(1L, 10L, 2L);

        verify(tasks).delete(deleted);
        assertThat(third.getSortOrder()).isEqualTo(2L);

        org.mockito.Mockito.doThrow(new RuntimeException("db")).when(tasks).flush();
        assertCode(ApiCode.TASK_DELETE_FAILED, () -> service.delete(1L, 10L, 2L));
    }

    private void stubMove(Task moving, BoardColumn column, List<Task> ordered) {
        when(projects.findWithLockByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(tasks.findOwned(moving.getId(), 10L, 1L)).thenReturn(Optional.of(moving));
        when(columns.findByIdAndProjectIdAndProjectUserId(column.getId(), 10L, 1L))
                .thenReturn(Optional.of(column));
        when(tasks.findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(10L, column.getId()))
                .thenReturn(ordered);
    }

    private Task task(long id, long columnId, long sortOrder, String title) {
        return id(new Task(10L, columnId, title, null, 1, sortOrder), id);
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
