package com.dewevrei.aikanban.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dewevrei.aikanban.domain.BoardColumn;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
    List<BoardColumn> findAllByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
    Optional<BoardColumn> findByIdAndProjectIdAndProjectUserId(Long id, Long projectId, Long userId);
    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(Long projectId, String name, Long id);
    long countByProjectId(Long projectId);
}
