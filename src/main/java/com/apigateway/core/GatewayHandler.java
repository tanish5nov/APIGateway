package com.apigateway.core;

import com.apigateway.auth.ApiKeyValidator;
import com.apigateway.circuit.CircuitBreaker;
import com.apigateway.logging.RequestLogger;
import com.apigateway.model.BackendInstance;
import com.apigateway.model.GatewayResponse;
import com.apigateway.model.LogEntry;
import com.apigateway.model.RequestContext;
import com.apigateway.ratelimit.RateLimiter;
import com.apigateway.lb.LoadBalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class GatewayHandler implements HttpHandler {
    private final ApiKeyValidator apiKeyValidator;
    private final RateLimiter rateLimiter;
    private final LoadBalancer loadBalancer;
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final RequestLogger requestLogger;
    private final BasicMetrics metrics;
    private final OverloadManager overloadManager;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GatewayHandler(ApiKeyValidator apiKeyValidator,
                          RateLimiter rateLimiter,
                          LoadBalancer loadBalancer,
                          Map<String, CircuitBreaker> circuitBreakers,
                          RequestLogger requestLogger,
                          BasicMetrics metrics,
                          OverloadManager overloadManager) {
        this.apiKeyValidator = apiKeyValidator;
        this.rateLimiter = rateLimiter;
        this.loadBalancer = loadBalancer;
        this.circuitBreakers = circuitBreakers;
        this.requestLogger = requestLogger;
        this.metrics = metrics;
        this.overloadManager = overloadManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        metrics.incTotal();

        String apiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getRawQuery();
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        long now = System.currentTimeMillis();

        RequestContext context = new RequestContext(apiKey, path, method, clientIp, query, now);

        if (overloadManager.isOverloaded()) {
            respond(exchange, new GatewayResponse(503, "Gateway overloaded"), null, apiKey, "REJECTED", "OVERLOADED");
            return;
        }

        if (!apiKeyValidator.isValid(apiKey)) {
            metrics.incAuthFailures();
            respond(exchange, new GatewayResponse(401, "Invalid API key"), null, apiKey, "REJECTED", "AUTH_FAILED");
            return;
        }

        if (!rateLimiter.allow(context)) {
            metrics.incRateLimitBlocked();
            respond(exchange, new GatewayResponse(429, "Rate limit exceeded"), null, apiKey, "REJECTED", "RATE_LIMIT");
            return;
        }

        Optional<BackendInstance> backendOpt = loadBalancer.selectBackend();
        if (backendOpt.isEmpty()) {
            respond(exchange, new GatewayResponse(503, "No healthy backends"), null, apiKey, "REJECTED", "NO_HEALTHY_BACKENDS");
            return;
        }

        BackendInstance backend = backendOpt.get();
        CircuitBreaker breaker = circuitBreakers.get(backend.getId());
        if (breaker == null) {
            respond(exchange, new GatewayResponse(503, "Circuit breaker missing"), backend.getId(), apiKey, "REJECTED", "CIRCUIT_MISSING");
            return;
        }

        if (!breaker.allowRequest()) {
            metrics.incCircuitOpenBlocked();
            respond(exchange, new GatewayResponse(503, "Circuit open for backend"), backend.getId(), apiKey, "REJECTED", "CIRCUIT_OPEN");
            return;
        }

        try {
            GatewayResponse response = forward(exchange, backend);
            if (response.getStatus() >= 500) {
                breaker.recordFailure();
                metrics.incBackendErrors();
                respond(exchange, response, backend.getId(), apiKey, "REJECTED", "BACKEND_ERROR");
            } else {
                breaker.recordSuccess();
                respond(exchange, response, backend.getId(), apiKey, "ACCEPTED", "SUCCESS");
            }
        } catch (Exception e) {
            breaker.recordFailure();
            metrics.incBackendErrors();
            respond(exchange, new GatewayResponse(502, "Backend error"), backend.getId(), apiKey, "REJECTED", "BACKEND_EXCEPTION");
        }
    }

    private GatewayResponse forward(HttpExchange exchange, BackendInstance backend) throws IOException, InterruptedException {
        URI uri = buildBackendUri(exchange.getRequestURI(), backend.getBaseUrl());
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).method(
                exchange.getRequestMethod(),
                HttpRequest.BodyPublishers.ofByteArray(readAllBytes(exchange.getRequestBody()))
        );

        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        String body = new String(response.body());
        return new GatewayResponse(response.statusCode(), body, Map.of("Content-Type", response.headers().firstValue("Content-Type").orElse("text/plain")));
    }

    private URI buildBackendUri(URI original, String baseUrl) {
        String path = original.getRawPath();
        String query = original.getRawQuery();
        String full = baseUrl + path + (query == null ? "" : "?" + query);
        return URI.create(full);
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    private void respond(HttpExchange exchange, GatewayResponse response, String backendId, String apiKey, String decision, String reason) throws IOException {
        response.getHeaders().forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
        if (backendId != null) {
            exchange.getResponseHeaders().set("X-Backend-Id", backendId);
        }
        exchange.getResponseHeaders().set("X-Decision", decision);
        exchange.getResponseHeaders().set("X-Reason", reason);
        byte[] body = response.getBody().getBytes();
        exchange.sendResponseHeaders(response.getStatus(), body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        requestLogger.log(new LogEntry(System.currentTimeMillis(), method, path, apiKey, backendId, response.getStatus(), decision, reason, response.getBody()));
    }
}
