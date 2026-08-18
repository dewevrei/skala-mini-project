package com.dewevrei.aikanban.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dewevrei.aikanban.domain.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    long countByColumnId(Long columnId);

    List<Task> findAllByProjectIdAndColumnIdOrderBySortOrderAscIdAsc(Long projectId, Long columnId);

    @Query("""
            select t from Task t
            join t.column c
            where t.projectId = :projectId
            order by c.sortOrder asc, c.id asc, t.sortOrder asc, t.id asc
            """)
    List<Task> findAllForItems(@Param("projectId") Long projectId);

    @Query("""
            select t from Task t
            join t.column c
            where t.projectId = :projectId
              and lower(t.title) like lower(concat('%', :title, '%')) escape '!'
            order by c.sortOrder asc, c.id asc, t.sortOrder asc, t.id asc
            """)
    List<Task> searchAllForItems(@Param("projectId") Long projectId, @Param("title") String title);

    @Query("""
            select t from Task t
            join t.column c
            join c.project p
            where t.id = :taskId and t.projectId = :projectId and p.user.id = :userId
            """)
    Optional<Task> findOwned(@Param("taskId") Long taskId,
            @Param("projectId") Long projectId, @Param("userId") Long userId);
}
