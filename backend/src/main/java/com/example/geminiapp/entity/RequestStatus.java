package com.example.geminiapp.entity;

/**
 * Enum representing the status of a request log entry
 */
public enum RequestStatus {
    PENDING("Request is being processed"),
    SUCCESS("Request completed successfully"),
    ERROR("Request failed with error");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}