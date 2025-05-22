package com.sp.schedulerplatform.model;

import java.sql.Timestamp;

public class Job {
    private int id;
    private String name;
    private String description;
    private Timestamp scheduledTime;
    private String executionMode;
    private int retryMax;
    private long retryDelayMs;
    private String concurrencyPolicy;
    private int createdBy;



    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getScheduledTime() {
        return scheduledTime;
    }
    public void setScheduledTime(Timestamp scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getExecutionMode() {
        return executionMode;
    }
    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public int getRetryMax() {
        return retryMax;
    }
    public void setRetryMax(int retryMax) {
        this.retryMax = retryMax;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public String getConcurrencyPolicy() {
        return concurrencyPolicy;
    }
    public void setConcurrencyPolicy(String concurrencyPolicy) {
        this.concurrencyPolicy = concurrencyPolicy;
    }

    public int getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }
}
