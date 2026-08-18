package com.dewevrei.aikanban.aitask;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AiGenerateRequest {
    private final String title;
    private final String description;
    private boolean unknownField;

    public AiGenerateRequest(@JsonProperty("title") String title,
            @JsonProperty("description") String description) {
        this.title = title;
        this.description = description;
    }

    public String title() { return title; }
    public String description() { return description; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) { unknownField = true; }
}
