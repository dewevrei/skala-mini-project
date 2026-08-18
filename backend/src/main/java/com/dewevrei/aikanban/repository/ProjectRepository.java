package com.dewevrei.aikanban.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.dewevrei.aikanban.domain.Project;

import jakarta.persistence.LockModeType;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Project> findWithLockByIdAndUserId(Long id, Long userId);
}
