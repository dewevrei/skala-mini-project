package com.dewevrei.aikanban.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.domain.User;

class RepositoryContractTest {

    @Test
    void 파생_query_method가_소유권_중복검사와_결정적_정렬을_명시한다() {
        assertThat(methodNames(ProjectRepository.class)).contains(
                "findAllByUserIdOrderByCreatedAtDescIdDesc",
                "findByIdAndUserId",
                "existsByUserIdAndNameIgnoreCase",
                "existsByUserIdAndNameIgnoreCaseAndIdNot",
                "findWithLockByIdAndUserId");
        assertThat(methodNames(BoardColumnRepository.class)).contains(
                "findAllByProjectIdOrderBySortOrderAscIdAsc",
                "findByIdAndProjectIdAndProjectUserId",
                "existsByProjectIdAndNameIgnoreCase",
                "existsByProjectIdAndNameIgnoreCaseAndIdNot",
                "countByProjectId");
        assertThat(methodNames(TaskRepository.class)).contains(
                "findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc");
    }

    @Test
    void Items_검색과_Task_소유권_JPQL이_경계와_정렬을_정확히_포함한다() throws NoSuchMethodException {
        String items = query(TaskRepository.class.getMethod("findAllForItems", Long.class));
        String search = query(TaskRepository.class.getMethod("searchAllForItems", Long.class, String.class));
        String owned = query(TaskRepository.class.getMethod("findOwned", Long.class, Long.class, Long.class));

        assertThat(items).contains("t.projectId = :projectId");
        assertThat(items).contains("order by c.sortOrder asc, c.id asc, t.sortOrder asc, t.id asc");
        assertThat(search).contains("t.projectId = :projectId");
        assertThat(search).contains("lower(t.title) like lower(concat('%', :title, '%'))");
        assertThat(search).contains("order by c.sortOrder asc, c.id asc, t.sortOrder asc, t.id asc");
        assertThat(owned).contains("t.id = :taskId", "t.projectId = :projectId", "p.user.id = :userId");
    }

    @Test
    void 소유권과_정렬_Repository_query를_파싱할_수_있다() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try (var sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Project.class)
                .addAnnotatedClass(BoardColumn.class)
                .addAnnotatedClass(Task.class)
                .buildMetadata()
                .buildSessionFactory();
             var entityManager = sessionFactory.createEntityManager()) {
            JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);

            assertThat(factory.getRepository(UserRepository.class)).isNotNull();
            assertThat(factory.getRepository(ProjectRepository.class)).isNotNull();
            assertThat(factory.getRepository(BoardColumnRepository.class)).isNotNull();
            assertThat(factory.getRepository(TaskRepository.class)).isNotNull();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static Set<String> methodNames(Class<?> repositoryType) {
        return Arrays.stream(repositoryType.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }

    private static String query(Method method) {
        return method.getAnnotation(Query.class).value().replaceAll("\\s+", " ").trim();
    }
}
