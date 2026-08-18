package com.dewevrei.aikanban.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TaskStatusRequest {
    private final Long targetColumnId;
    private boolean unknownField;

    public TaskStatusRequest(@JsonProperty("targetColumnId") Long targetColumnId) {
        this.targetColumnId = targetColumnId;
    }

    public Long targetColumnId() { return targetColumnId; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) { unknownField = true; }
}
