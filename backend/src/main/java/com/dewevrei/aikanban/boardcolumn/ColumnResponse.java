package com.dewevrei.aikanban.boardcolumn;

import com.dewevrei.aikanban.domain.BoardColumn;

public record ColumnResponse(Long id, Long projectId, String name, int sortOrder, long taskCount) {
    public static ColumnResponse from(BoardColumn column, long taskCount) {
        return new ColumnResponse(column.getId(), column.getProject().getId(), column.getName(),
                column.getSortOrder(), taskCount);
    }
}
