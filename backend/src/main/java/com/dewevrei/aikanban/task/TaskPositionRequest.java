package com.dewevrei.aikanban.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TaskPositionRequest {
    private final Long targetColumnId;
    private final Long beforeTaskId;
    private boolean unknownField;

    public TaskPositionRequest(@JsonProperty("targetColumnId") Long targetColumnId,
            @JsonProperty("beforeTaskId") Long beforeTaskId) {
        this.targetColumnId = targetColumnId;
        this.beforeTaskId = beforeTaskId;
    }

    public Long targetColumnId() { return targetColumnId; }
    public Long beforeTaskId() { return beforeTaskId; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) { unknownField = true; }
}
