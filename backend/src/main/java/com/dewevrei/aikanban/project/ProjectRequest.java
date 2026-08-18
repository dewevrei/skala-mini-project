package com.dewevrei.aikanban.project;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ProjectRequest {
    private final String name;
    private final String description;
    private boolean readOnlyField;
    private boolean unknownField;

    public ProjectRequest(@JsonProperty("name") String name,
            @JsonProperty("description") String description) {
        this.name = name;
        this.description = description;
    }

    public String name() { return name; }
    public String description() { return description; }
    public boolean hasReadOnlyField() { return readOnlyField; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) {
        if (Map.of("id", true, "userId", true, "createdAt", true, "updatedAt", true,
                "columns", true).containsKey(field)) {
            readOnlyField = true;
        } else {
            unknownField = true;
        }
    }
}
