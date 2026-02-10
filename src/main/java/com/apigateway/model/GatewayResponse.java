package com.apigateway.model;

import java.util.Collections;
import java.util.Map;

public class GatewayResponse {
    private final int status;
    private final String body;
    private final Map<String, String> headers;

    public GatewayResponse(int status, String body) {
        this(status, body, Collections.emptyMap());
    }

    public GatewayResponse(int status, String body, Map<String, String> headers) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
