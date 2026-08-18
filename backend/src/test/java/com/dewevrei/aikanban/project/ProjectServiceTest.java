package com.dewevrei.aikanban.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.User;
import com.dewevrei.aikanban.repository.BoardColumnRepository;
import com.dewevrei.aikanban.repository.ProjectRepository;
import com.dewevrei.aikanban.repository.TaskRepository;
import com.dewevrei.aikanban.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectRepository projects;
    @Mock BoardColumnRepository columns;
    @Mock TaskRepository tasks;
    @Mock UserRepository users;
    private ProjectService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projects, columns, tasks, users);
        owner = user(1L);
    }

    @Test
    void 생성은_Project와_기본열_세개를_같은_transaction에서_저장한다() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(projects.saveAndFlush(any())).thenAnswer(call -> id(call.getArgument(0), 10L));
        when(columns.saveAllAndFlush(any())).thenAnswer(call -> {
            List<BoardColumn> result = call.getArgument(0);
            for (int i = 0; i < result.size(); i++) id(result.get(i), 100L + i);
            return result;
        });

        var result = service.create(1L, new ProjectRequest(" 새 프로젝트 ", " 설명 "));

        assertThat(result.project().name()).isEqualTo("새 프로젝트");
        assertThat(result.columns()).extracting("name").containsExactly("Todo", "In Progress", "Done");
        assertThat(result.columns()).extracting("sortOrder").containsExactly(1, 2, 3);
        Method create = ProjectService.class.getMethod("create", long.class, ProjectRequest.class);
        assertThat(create.getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void 기본열_저장실패는_Project_생성실패로_전파되어_transaction_rollback을_유도한다() {
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(projects.saveAndFlush(any())).thenAnswer(call -> id(call.getArgument(0), 10L));
        when(columns.saveAllAndFlush(any())).thenThrow(new IllegalStateException("db"));

        assertCode(ApiCode.PROJECT_CREATE_FAILED,
                () -> service.create(1L, new ProjectRequest("프로젝트", null)));
    }

    @Test
    void 목록은_repository의_소유자별_최근순_query_결과를_그대로_유지한다() {
        Project newer = project(2L, owner, "둘째");
        Project older = project(1L, owner, "첫째");
        when(projects.findAllByUserIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(newer, older));

        assertThat(service.list(1L)).extracting(ProjectResponse::id).containsExactly(2L, 1L);
    }

    @Test
    void 타인_Project는_존재를_숨기고_이름은_owner_범위에서_대소문자_없이_중복검사한다() {
        when(projects.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.PROJECT_NOT_FOUND, () -> service.get(1L, 9L));

        when(projects.existsByUserIdAndNameIgnoreCase(1L, "alpha")).thenReturn(true);
        assertCode(ApiCode.DUPLICATE_PROJECT_NAME,
                () -> service.create(1L, new ProjectRequest(" alpha ", null)));
    }

    @Test
    void 수정은_자기자신과_같은_이름을_허용하고_삭제는_DB_cascade가_동작하도록_Project를_삭제한다() {
        Project project = project(10L, owner, "Alpha");
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(project));
        when(projects.saveAndFlush(project)).thenReturn(project);

        assertThat(service.update(1L, 10L, new ProjectRequest("alpha", null)).name()).isEqualTo("alpha");
        service.delete(1L, 10L);

        verify(projects).existsByUserIdAndNameIgnoreCaseAndIdNot(1L, "alpha", 10L);
        verify(projects).delete(project);
        verify(projects).flush();
    }

    private User user(long id) {
        User user = new User("g" + id, "name", id + "@example.com", "nick" + id);
        return id(user, id);
    }

    private Project project(long id, User user, String name) {
        Project project = id(new Project(user, name, null), id);
        ReflectionTestUtils.setField(project, "createdAt", LocalDateTime.of(2026, 8, 18, 1, 0));
        ReflectionTestUtils.setField(project, "updatedAt", LocalDateTime.of(2026, 8, 18, 1, 0));
        return project;
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
