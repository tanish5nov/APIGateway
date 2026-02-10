package com.apigateway.config;

import com.apigateway.model.BackendInstance;

import java.util.List;
import java.util.Set;

public class GatewayConfig {
    public enum RateLimitStrategy {
        TOKEN_BUCKET,
        SLIDING_WINDOW
    }

    private final int port;
    private final Set<String> apiKeys;
    private final RateLimitStrategy rateLimitStrategy;
    private final int tokenBucketCapacity;
    private final double tokenBucketRefillPerSecond;
    private final int slidingWindowMaxRequests;
    private final int slidingWindowSeconds;
    private final int circuitFailureThreshold;
    private final long circuitResetTimeoutMillis;
    private final List<BackendInstance> backends;
    private final long healthCheckIntervalMillis;
    private final long healthCheckTimeoutMillis;
    private final String healthCheckPath;

    public GatewayConfig(int port,
                         Set<String> apiKeys,
                         RateLimitStrategy rateLimitStrategy,
                         int tokenBucketCapacity,
                         double tokenBucketRefillPerSecond,
                         int slidingWindowMaxRequests,
                         int slidingWindowSeconds,
                         int circuitFailureThreshold,
                         long circuitResetTimeoutMillis,
                         List<BackendInstance> backends,
                         long healthCheckIntervalMillis,
                         long healthCheckTimeoutMillis,
                         String healthCheckPath) {
        this.port = port;
        this.apiKeys = apiKeys;
        this.rateLimitStrategy = rateLimitStrategy;
        this.tokenBucketCapacity = tokenBucketCapacity;
        this.tokenBucketRefillPerSecond = tokenBucketRefillPerSecond;
        this.slidingWindowMaxRequests = slidingWindowMaxRequests;
        this.slidingWindowSeconds = slidingWindowSeconds;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitResetTimeoutMillis = circuitResetTimeoutMillis;
        this.backends = backends;
        this.healthCheckIntervalMillis = healthCheckIntervalMillis;
        this.healthCheckTimeoutMillis = healthCheckTimeoutMillis;
        this.healthCheckPath = healthCheckPath;
    }

    public int getPort() {
        return port;
    }

    public Set<String> getApiKeys() {
        return apiKeys;
    }

    public RateLimitStrategy getRateLimitStrategy() {
        return rateLimitStrategy;
    }

    public int getTokenBucketCapacity() {
        return tokenBucketCapacity;
    }

    public double getTokenBucketRefillPerSecond() {
        return tokenBucketRefillPerSecond;
    }

    public int getSlidingWindowMaxRequests() {
        return slidingWindowMaxRequests;
    }

    public int getSlidingWindowSeconds() {
        return slidingWindowSeconds;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public long getCircuitResetTimeoutMillis() {
        return circuitResetTimeoutMillis;
    }

    public List<BackendInstance> getBackends() {
        return backends;
    }

    public long getHealthCheckIntervalMillis() {
        return healthCheckIntervalMillis;
    }

    public long getHealthCheckTimeoutMillis() {
        return healthCheckTimeoutMillis;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public static GatewayConfig defaultConfig() {
        return new GatewayConfig(
                8080,
                Set.of("demo-key-1", "demo-key-2"),
                RateLimitStrategy.TOKEN_BUCKET,
                10,
                5.0,
                20,
                10,
                3,
                10_000,
                List.of(
                        new BackendInstance("backend-1", "http://localhost:9001"),
                        new BackendInstance("backend-2", "http://localhost:9002")
                ),
                5_000,
                2_000,
                "/health"
        );
    }
}
