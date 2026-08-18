package com.dewevrei.aikanban.boardcolumn;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ReorderColumnsRequest {
    private static final Set<String> READ_ONLY = Set.of("projectId", "columns");
    private final List<Long> orderedColumnIds;
    private boolean readOnlyField;
    private boolean unknownField;

    public ReorderColumnsRequest(@JsonProperty("orderedColumnIds") List<Long> orderedColumnIds) {
        this.orderedColumnIds = orderedColumnIds;
    }

    public List<Long> orderedColumnIds() { return orderedColumnIds; }
    public boolean hasReadOnlyField() { return readOnlyField; }
    public boolean hasUnknownField() { return unknownField; }

    @JsonAnySetter
    void captureUnknown(String field, Object value) {
        if (READ_ONLY.contains(field)) readOnlyField = true;
        else unknownField = true;
    }
}
