package com.dewevrei.aikanban.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "board_columns", uniqueConstraints = {
        @UniqueConstraint(name = "uk_board_columns_project_name", columnNames = {"project_id", "name"}),
        @UniqueConstraint(name = "uk_board_columns_project_id_id", columnNames = {"project_id", "id"})
})
public class BoardColumn extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected BoardColumn() {
    }

    public BoardColumn(Project project, String name, int sortOrder) {
        this.project = project;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }

    public void rename(String name) { this.name = name; }
    public void reorder(int sortOrder) { this.sortOrder = sortOrder; }
}
