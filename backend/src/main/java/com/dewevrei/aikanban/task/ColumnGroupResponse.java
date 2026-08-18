package com.dewevrei.aikanban.task;

import java.util.List;

import com.dewevrei.aikanban.boardcolumn.ColumnResponse;

public record ColumnGroupResponse(ColumnResponse column, List<TaskResponse> tasks) {}
