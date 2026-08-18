package com.dewevrei.aikanban.boardcolumn;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ColumnRequest {
    private static final Set<String> READ_ONLY = Set.of("id", "projectId", "sortOrder", "taskCount",
            "createdAt", "updatedAt");
    private final String name;
    private boolean readOnlyField;
    private boolean unknownField;

    public ColumnRequest(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String name() { return name; }
    public boolean hasReadOnlyField() { return readOnlyField; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) {
        if (READ_ONLY.contains(field)) readOnlyField = true;
        else unknownField = true;
    }
}
