package com.dewevrei.aikanban.domain;

import java.time.LocalDate;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
@Check(name = "chk_tasks_priority", constraints = "priority BETWEEN 1 AND 5")
public class Task extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "column_id", nullable = false)
    private Long columnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false,
                    insertable = false, updatable = false),
            @JoinColumn(name = "column_id", referencedColumnName = "id", nullable = false,
                    insertable = false, updatable = false)
    })
    private BoardColumn column;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private int priority;

    @Column(name = "sort_order", nullable = false)
    private long sortOrder;

    protected Task() {
    }

    public Task(Long projectId, Long columnId, String title, String description, int priority, long sortOrder) {
        this.projectId = projectId;
        this.columnId = columnId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getColumnId() { return columnId; }
    public BoardColumn getColumn() { return column; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getPriority() { return priority; }
    public long getSortOrder() { return sortOrder; }

    public void updateContent(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updatePriority(int priority) {
        this.priority = priority;
    }

    public void moveTo(Long projectId, Long columnId, long sortOrder) {
        this.projectId = projectId;
        this.columnId = columnId;
        this.sortOrder = sortOrder;
    }

    public void reorder(long sortOrder) { this.sortOrder = sortOrder; }
}
