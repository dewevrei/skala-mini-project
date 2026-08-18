package com.dewevrei.aikanban.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public final class TaskDatesRequest {
    private String startDate;
    private String endDate;
    private boolean startDatePresent;
    private boolean endDatePresent;
    private boolean unknownField;

    public TaskDatesRequest() {}

    public TaskDatesRequest(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.startDatePresent = true;
        this.endDatePresent = true;
    }

    @JsonSetter("startDate")
    void setStartDate(String startDate) {
        this.startDate = startDate;
        this.startDatePresent = true;
    }

    @JsonSetter("endDate")
    void setEndDate(String endDate) {
        this.endDate = endDate;
        this.endDatePresent = true;
    }

    @JsonAnySetter
    void captureUnknown(String field, Object value) { unknownField = true; }

    public String startDate() { return startDate; }
    public String endDate() { return endDate; }
    public boolean hasBothKeys() { return startDatePresent && endDatePresent; }
    public boolean hasUnknownField() { return unknownField; }
}
