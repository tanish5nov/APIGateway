package com.apigateway.model;

public class RequestContext {
    private final String apiKey;
    private final String path;
    private final String method;
    private final String clientIp;
    private final String query;
    private final long timestampMillis;

    public RequestContext(String apiKey, String path, String method, String clientIp, String query, long timestampMillis) {
        this.apiKey = apiKey;
        this.path = path;
        this.method = method;
        this.clientIp = clientIp;
        this.query = query;
        this.timestampMillis = timestampMillis;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getQuery() {
        return query;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
