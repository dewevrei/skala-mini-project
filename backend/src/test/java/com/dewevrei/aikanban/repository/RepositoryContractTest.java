package com.dewevrei.aikanban.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import com.dewevrei.aikanban.domain.BoardColumn;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.domain.Task;
import com.dewevrei.aikanban.domain.User;

class RepositoryContractTest {

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
}
