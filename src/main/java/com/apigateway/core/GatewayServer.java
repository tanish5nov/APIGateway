package com.apigateway.core;

import com.apigateway.auth.ApiKeyValidator;
import com.apigateway.auth.StaticApiKeyValidator;
import com.apigateway.circuit.CircuitBreaker;
import com.apigateway.circuit.DefaultCircuitBreaker;
import com.apigateway.config.GatewayConfig;
import com.apigateway.health.HealthChecker;
import com.apigateway.lb.LoadBalancer;
import com.apigateway.lb.RoundRobinLoadBalancer;
import com.apigateway.logging.InMemoryRequestLogger;
import com.apigateway.logging.RequestLogger;
import com.apigateway.model.BackendInstance;
import com.apigateway.ratelimit.RateLimiter;
import com.apigateway.ratelimit.SlidingWindowLimiter;
import com.apigateway.ratelimit.TokenBucketLimiter;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class GatewayServer {
    public static void main(String[] args) throws IOException {
        GatewayConfig config = GatewayConfig.defaultConfig();

        ApiKeyValidator apiKeyValidator = new StaticApiKeyValidator(config.getApiKeys());
        RateLimiter rateLimiter = createRateLimiter(config);
        RequestLogger requestLogger = new InMemoryRequestLogger(200);
        BasicMetrics metrics = new BasicMetrics();
        OverloadManager overloadManager = new OverloadManager();

        HealthChecker healthChecker = new HealthChecker(
                config.getBackends(),
                config.getHealthCheckIntervalMillis(),
                config.getHealthCheckTimeoutMillis(),
                config.getHealthCheckPath()
        );
        healthChecker.start();

        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(config.getBackends(), healthChecker);
        Map<String, CircuitBreaker> circuitBreakers = new HashMap<>();
        for (BackendInstance backend : config.getBackends()) {
            circuitBreakers.put(backend.getId(), new DefaultCircuitBreaker(
                    config.getCircuitFailureThreshold(),
                    config.getCircuitResetTimeoutMillis()
            ));
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
        server.createContext("/admin", new AdminHandler(metrics, healthChecker, circuitBreakers, requestLogger, overloadManager));
        server.createContext("/ui", new StaticPageHandler("/ui/index.html", "text/html; charset=utf-8"));
        server.createContext("/simulate", new StaticPageHandler("/ui/simulate.html", "text/html; charset=utf-8"));
        server.createContext("/", new GatewayHandler(apiKeyValidator, rateLimiter, loadBalancer, circuitBreakers, requestLogger, metrics, overloadManager));
        server.setExecutor(null);
        server.start();

        System.out.println("API Gateway running on port " + config.getPort());
        System.out.println("UI: http://localhost:" + config.getPort() + "/ui");
        System.out.println("Simulate: http://localhost:" + config.getPort() + "/simulate");
    }

    private static RateLimiter createRateLimiter(GatewayConfig config) {
        if (config.getRateLimitStrategy() == GatewayConfig.RateLimitStrategy.SLIDING_WINDOW) {
            return new SlidingWindowLimiter(config.getSlidingWindowMaxRequests(), config.getSlidingWindowSeconds());
        }
        return new TokenBucketLimiter(config.getTokenBucketCapacity(), config.getTokenBucketRefillPerSecond());
    }
}
