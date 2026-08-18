package com.dewevrei.aikanban.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TaskPriorityRequest {
    private final Integer priority;
    private boolean unknownField;

    public TaskPriorityRequest(@JsonProperty("priority") Integer priority) {
        this.priority = priority;
    }

    public Integer priority() { return priority; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) {
        unknownField = true;
    }
}
