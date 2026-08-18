package com.dewevrei.aikanban.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
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
    void 모든_ID는_BIGINT_identity_자동_증가_계약이다() throws NoSuchFieldException {
        for (Class<?> type : new Class<?>[] {User.class, Project.class, BoardColumn.class, Task.class}) {
            Field id = type.getDeclaredField("id");
            assertThat(id.getType()).as(type.getSimpleName()).isEqualTo(Long.class);
            assertThat(id.getAnnotation(GeneratedValue.class).strategy())
                    .as(type.getSimpleName()).isEqualTo(GenerationType.IDENTITY);
        }
    }

    @Test
    void 엔티티의_필드_집합과_Java_타입이_확정_계약과_일치한다() {
        assertThat(fieldSignatures(BaseTimeEntity.class)).containsExactlyInAnyOrder(
                "createdAt:" + LocalDateTime.class.getName(), "updatedAt:" + LocalDateTime.class.getName());
        assertThat(fieldSignatures(User.class)).containsExactlyInAnyOrder(
                "id:java.lang.Long", "googleId:java.lang.String", "name:java.lang.String",
                "email:java.lang.String", "nickname:java.lang.String");
        assertThat(fieldSignatures(Project.class)).containsExactlyInAnyOrder(
                "id:java.lang.Long", "user:" + User.class.getName(), "name:java.lang.String",
                "description:java.lang.String");
        assertThat(fieldSignatures(BoardColumn.class)).containsExactlyInAnyOrder(
                "id:java.lang.Long", "project:" + Project.class.getName(), "name:java.lang.String",
                "sortOrder:int");
        assertThat(fieldSignatures(Task.class)).containsExactlyInAnyOrder(
                "id:java.lang.Long", "projectId:java.lang.Long", "columnId:java.lang.Long",
                "column:" + BoardColumn.class.getName(), "title:java.lang.String", "description:java.lang.String",
                "startDate:" + LocalDate.class.getName(), "endDate:" + LocalDate.class.getName(),
                "priority:int", "sortOrder:long");
    }

    @Test
    void 모든_필드의_컬럼명_nullability와_길이가_DDL과_일치한다() throws NoSuchFieldException {
        assertVarchar(User.class, "googleId", "google_id", false, 255);
        assertVarchar(User.class, "name", "name", false, 255);
        assertVarchar(User.class, "email", "email", false, 320);
        assertVarchar(User.class, "nickname", "nickname", false, 255);

        assertVarchar(Project.class, "name", "name", false, 100);
        assertColumn(Project.class, "description", "description", true);

        assertVarchar(BoardColumn.class, "name", "name", false, 50);
        assertColumn(BoardColumn.class, "sortOrder", "sort_order", false);

        assertColumn(Task.class, "projectId", "project_id", false);
        assertColumn(Task.class, "columnId", "column_id", false);
        assertVarchar(Task.class, "title", "title", false, 200);
        assertColumn(Task.class, "description", "description", true);
        assertColumn(Task.class, "startDate", "start_date", true);
        assertColumn(Task.class, "endDate", "end_date", true);
        assertColumn(Task.class, "priority", "priority", false);
        assertColumn(Task.class, "sortOrder", "sort_order", false);
        assertThat(Project.class.getDeclaredField("description").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("TEXT");
        assertThat(Task.class.getDeclaredField("description").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("TEXT");
        assertThat(Task.class.getDeclaredField("priority").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("TINYINT UNSIGNED");
    }

    @Test
    void timestamp는_DATETIME6_nonnull이며_생성_수정_시점이_분리된다() throws NoSuchFieldException {
        Field createdAt = BaseTimeEntity.class.getDeclaredField("createdAt");
        Field updatedAt = BaseTimeEntity.class.getDeclaredField("updatedAt");

        assertThat(createdAt.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(createdAt.getAnnotation(Column.class).updatable()).isFalse();
        assertThat(createdAt.getAnnotation(Column.class).columnDefinition()).isEqualTo("DATETIME(6)");
        assertThat(createdAt.getAnnotation(CreationTimestamp.class)).isNotNull();
        assertThat(updatedAt.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(updatedAt.getAnnotation(Column.class).columnDefinition()).isEqualTo("DATETIME(6)");
        assertThat(updatedAt.getAnnotation(UpdateTimestamp.class)).isNotNull();
    }

    @Test
    void 소유_관계의_FK_컬럼은_nonnull이다() throws NoSuchFieldException {
        assertJoin(Project.class, "user", "user_id");
        assertJoin(BoardColumn.class, "project", "project_id");
    }

    @Test
    void 엔티티_unique_key가_DDL_계약과_일치한다() {
        assertThat(uniqueKeys(User.class)).containsExactlyInAnyOrder(
                "uk_users_google_id:google_id", "uk_users_email:email", "uk_users_nickname:nickname");
        assertThat(uniqueKeys(Project.class)).containsExactly(
                "uk_projects_user_name:user_id,name");
        assertThat(uniqueKeys(BoardColumn.class)).containsExactlyInAnyOrder(
                "uk_board_columns_project_name:project_id,name",
                "uk_board_columns_project_id_id:project_id,id");
        assertThat(uniqueKeys(Task.class)).isEmpty();
    }

    @Test
    void Task는_project와_column_복합_참조를_가진다() throws NoSuchFieldException {
        Field column = Task.class.getDeclaredField("column");
        JoinColumns joins = column.getAnnotation(JoinColumns.class);

        assertThat(joins.value()).extracting(join -> join.name())
                .containsExactly("project_id", "column_id");
        assertThat(joins.value()).extracting(join -> join.referencedColumnName())
                .containsExactly("project_id", "id");
        assertThat(joins.value()).allSatisfy(join -> {
            assertThat(join.nullable()).isFalse();
            assertThat(join.insertable()).isFalse();
            assertThat(join.updatable()).isFalse();
        });
        assertThat(column.getAnnotation(ManyToOne.class).optional()).isFalse();
    }

    @Test
    void 수동_DDL은_ERD_확정본과_정확히_일치하고_FK_cascade와_restrict를_포함한다() throws IOException {
        String erd = Files.readString(Path.of("..", "docs", "erd.md"));
        String ddl = normalizeLines(Files.readString(Path.of("..", "database", "ai_kanban.sql"))).trim();
        var match = Pattern.compile("```sql\\R(?<sql>[\\s\\S]*?)\\R```", Pattern.MULTILINE).matcher(erd);

        assertThat(match.find()).isTrue();
        assertThat(ddl).isEqualTo(normalizeLines(match.group("sql")).trim());
        assertThat(ddl).contains("COLLATE utf8mb4_0900_ai_ci", "ON DELETE RESTRICT");
        assertThat(ddl).containsSubsequence(
                "CONSTRAINT fk_board_columns_project", "ON DELETE CASCADE");
        assertThat(ddl).containsSubsequence(
                "CONSTRAINT fk_tasks_project_column", "FOREIGN KEY (project_id, column_id)",
                "REFERENCES board_columns (project_id, id)", "ON DELETE CASCADE");
        assertThat(ddl).contains("CONSTRAINT chk_tasks_priority CHECK (priority BETWEEN 1 AND 5)");
    }

    @Test
    void application은_DDL_자동_변경을_금지하고_서울_시간대를_사용한다() throws IOException {
        String yaml = normalizeLines(Files.readString(Path.of("src", "main", "resources", "application.yml")));

        assertThat(yaml).contains("ddl-auto: validate");
        assertThat(yaml).doesNotContain("ddl-auto: create", "ddl-auto: update", "ddl-auto: create-drop");
        assertThat(yaml).contains("time_zone: Asia/Seoul", "connectionTimeZone=Asia/Seoul",
                "forceConnectionTimeZoneToSession=true", "timezone: Asia/Seoul");
    }

    private static void assertColumn(Class<?> type, String fieldName, String expectedName,
            boolean nullable) throws NoSuchFieldException {
        Column column = type.getDeclaredField(fieldName).getAnnotation(Column.class);
        String actualName = column.name().isBlank() ? fieldName : column.name();
        assertThat(actualName).as(type.getSimpleName() + "." + fieldName).isEqualTo(expectedName);
        assertThat(column.nullable()).as(type.getSimpleName() + "." + fieldName).isEqualTo(nullable);
    }

    private static void assertVarchar(Class<?> type, String fieldName, String expectedName,
            boolean nullable, int length) throws NoSuchFieldException {
        assertColumn(type, fieldName, expectedName, nullable);
        Column column = type.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertThat(column.length()).as(type.getSimpleName() + "." + fieldName).isEqualTo(length);
    }

    private static void assertJoin(Class<?> type, String fieldName, String expectedName)
            throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        JoinColumn join = field.getAnnotation(JoinColumn.class);
        assertThat(join.name()).isEqualTo(expectedName);
        assertThat(join.nullable()).isFalse();
        assertThat(field.getAnnotation(ManyToOne.class).optional()).isFalse();
    }

    private static Set<String> uniqueKeys(Class<?> type) {
        return Arrays.stream(type.getAnnotation(Table.class).uniqueConstraints())
                .map(unique -> unique.name() + ":" + String.join(",", unique.columnNames()))
                .collect(Collectors.toSet());
    }

    private static Set<String> fieldSignatures(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(field -> field.getName() + ":" + field.getType().getName())
                .collect(Collectors.toSet());
    }

    private static String normalizeLines(String value) {
        return value.replace("\r\n", "\n");
    }
}
