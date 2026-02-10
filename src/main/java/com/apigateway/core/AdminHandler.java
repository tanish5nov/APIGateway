package com.apigateway.core;

import com.apigateway.circuit.CircuitBreaker;
import com.apigateway.logging.RequestLogger;
import com.apigateway.model.LogEntry;
import com.apigateway.health.HealthChecker;
import com.apigateway.model.HealthStatus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class AdminHandler implements HttpHandler {
    private final BasicMetrics metrics;
    private final HealthChecker healthChecker;
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final RequestLogger requestLogger;
    private final OverloadManager overloadManager;

    public AdminHandler(BasicMetrics metrics,
                        HealthChecker healthChecker,
                        Map<String, CircuitBreaker> circuitBreakers,
                        RequestLogger requestLogger,
                        OverloadManager overloadManager) {
        this.metrics = metrics;
        this.healthChecker = healthChecker;
        this.circuitBreakers = circuitBreakers;
        this.requestLogger = requestLogger;
        this.overloadManager = overloadManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String response;

        if (path.endsWith("/health")) {
            response = healthJson();
        } else if (path.endsWith("/circuit")) {
            response = circuitJson();
        } else if (path.endsWith("/ratelimits")) {
            response = rateLimitJson();
        } else if (path.endsWith("/logs")) {
            response = logsJson();
        } else if (path.endsWith("/metrics")) {
            response = metricsJson();
        } else if (path.endsWith("/reset")) {
            response = resetAll();
        } else if (path.endsWith("/overload")) {
            response = overloadToggle(exchange.getRequestURI().getRawQuery());
        } else {
            response = "{\"error\":\"Unknown admin endpoint\"}";
        }

        byte[] body = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String healthJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"health\":[");
        boolean first = true;
        for (Map.Entry<String, HealthStatus> entry : healthChecker.getAllHealth().entrySet()) {
            if (!first) sb.append(",");
            sb.append("{\"backendId\":\"").append(entry.getKey()).append("\",");
            sb.append("\"status\":\"").append(entry.getValue()).append("\"}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String circuitJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"circuits\":[");
        boolean first = true;
        for (Map.Entry<String, CircuitBreaker> entry : circuitBreakers.entrySet()) {
            if (!first) sb.append(",");
            sb.append("{\"backendId\":\"").append(entry.getKey()).append("\",");
            sb.append("\"state\":\"").append(entry.getValue().getState()).append("\"}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String rateLimitJson() {
        return "{\"rateLimitBlocked\":" + metrics.getRateLimitBlocked() + ",\"totalRequests\":" + metrics.getTotalRequests() + "}";
    }

    private String metricsJson() {
        return "{\"totalRequests\":" + metrics.getTotalRequests()
                + ",\"authFailures\":" + metrics.getAuthFailures()
                + ",\"rateLimitBlocked\":" + metrics.getRateLimitBlocked()
                + ",\"circuitOpenBlocked\":" + metrics.getCircuitOpenBlocked()
                + ",\"backendErrors\":" + metrics.getBackendErrors()
                + ",\"overloaded\":" + overloadManager.isOverloaded() + "}";
    }

    private String logsJson() {
        List<LogEntry> entries = requestLogger.recent();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"logs\":[");
        boolean first = true;
        for (LogEntry entry : entries) {
            if (!first) sb.append(",");
            sb.append("{");
            sb.append("\"timestamp\":").append(entry.getTimestampMillis()).append(",");
            sb.append("\"method\":\"").append(entry.getMethod()).append("\",");
            sb.append("\"path\":\"").append(entry.getPath()).append("\",");
            sb.append("\"apiKey\":\"").append(entry.getApiKey()).append("\",");
            sb.append("\"backendId\":\"").append(entry.getBackendId()).append("\",");
            sb.append("\"status\":").append(entry.getStatus()).append(",");
            sb.append("\"decision\":\"").append(entry.getDecision()).append("\",");
            sb.append("\"reason\":\"").append(entry.getReason()).append("\"");
            sb.append("}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    private String resetAll() {
        healthChecker.forceAllHealthy();
        for (CircuitBreaker breaker : circuitBreakers.values()) {
            breaker.reset();
        }
        metrics.reset();
        overloadManager.setOverloaded(false);
        return "{\"status\":\"ok\",\"message\":\"health and circuits reset\"}";
    }

    private String overloadToggle(String query) {
        if (query != null && query.contains("state=on")) {
            overloadManager.setOverloaded(true);
        } else if (query != null && query.contains("state=off")) {
            overloadManager.setOverloaded(false);
        }
        return "{\"status\":\"ok\",\"overloaded\":" + overloadManager.isOverloaded() + "}";
    }
}
