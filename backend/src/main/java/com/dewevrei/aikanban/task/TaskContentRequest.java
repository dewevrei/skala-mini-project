package com.dewevrei.aikanban.task;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class TaskContentRequest {
    private static final Set<String> READ_ONLY = Set.of("id", "projectId", "columnId", "priority",
            "sortOrder", "startDate", "endDate", "createdAt", "updatedAt");
    private final String title;
    private final String description;
    private boolean readOnlyField;
    private boolean unknownField;

    public TaskContentRequest(@JsonProperty("title") String title,
            @JsonProperty("description") String description) {
        this.title = title;
        this.description = description;
    }

    public String title() { return title; }
    public String description() { return description; }
    public boolean hasReadOnlyField() { return readOnlyField; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) {
        if (READ_ONLY.contains(field)) readOnlyField = true;
        else unknownField = true;
    }
}
