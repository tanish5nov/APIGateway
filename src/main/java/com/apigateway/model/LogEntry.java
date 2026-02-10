package com.apigateway.model;

public class LogEntry {
    private final long timestampMillis;
    private final String method;
    private final String path;
    private final String apiKey;
    private final String backendId;
    private final int status;
    private final String decision;
    private final String reason;
    private final String message;

    public LogEntry(long timestampMillis, String method, String path, String apiKey, String backendId, int status, String decision, String reason, String message) {
        this.timestampMillis = timestampMillis;
        this.method = method;
        this.path = path;
        this.apiKey = apiKey;
        this.backendId = backendId;
        this.status = status;
        this.decision = decision;
        this.reason = reason;
        this.message = message;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBackendId() {
        return backendId;
    }

    public int getStatus() {
        return status;
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }
}
