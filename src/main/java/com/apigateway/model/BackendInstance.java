package com.apigateway.model;

public class BackendInstance {
    private final String id;
    private final String baseUrl;

    public BackendInstance(String id, String baseUrl) {
        this.id = id;
        this.baseUrl = baseUrl;
    }

    public String getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
