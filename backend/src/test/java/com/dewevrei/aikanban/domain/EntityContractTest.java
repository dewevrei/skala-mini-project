package com.dewevrei.aikanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.Table;

class EntityContractTest {

    @Test
    void Hibernate가_DB_연결_없이_전체_매핑을_해석한다() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            var metadata = new MetadataSources(registry)
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(Project.class)
                    .addAnnotatedClass(BoardColumn.class)
                    .addAnnotatedClass(Task.class)
                    .buildMetadata();
            assertThat(metadata.getEntityBindings()).hasSize(4);
            metadata.buildSessionFactory().close();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void 엔티티가_확정된_테이블을_사용한다() {
        assertThat(User.class.getAnnotation(Table.class).name()).isEqualTo("users");
        assertThat(Project.class.getAnnotation(Table.class).name()).isEqualTo("projects");
        assertThat(BoardColumn.class.getAnnotation(Table.class).name()).isEqualTo("board_columns");
        assertThat(Task.class.getAnnotation(Table.class).name()).isEqualTo("tasks");
    }

    @Test
    void Task는_project와_column_복합_참조를_가진다() throws NoSuchFieldException {
        Field column = Task.class.getDeclaredField("column");
        JoinColumns joins = column.getAnnotation(JoinColumns.class);

        assertThat(joins.value()).extracting(join -> join.name())
                .containsExactly("project_id", "column_id");
        assertThat(joins.value()).extracting(join -> join.referencedColumnName())
                .containsExactly("project_id", "id");
    }

    @Test
    void 문자열_길이가_DDL과_일치한다() throws NoSuchFieldException {
        assertThat(Project.class.getDeclaredField("name").getAnnotation(Column.class).length()).isEqualTo(100);
        assertThat(BoardColumn.class.getDeclaredField("name").getAnnotation(Column.class).length()).isEqualTo(50);
        assertThat(Task.class.getDeclaredField("title").getAnnotation(Column.class).length()).isEqualTo(200);
    }
}
