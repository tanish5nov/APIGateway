package com.apigateway.health;

import com.apigateway.model.BackendInstance;
import com.apigateway.model.HealthStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthChecker {
    private final List<BackendInstance> backends;
    private final Map<String, HealthStatus> healthMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final HttpClient client = HttpClient.newHttpClient();
    private final long intervalMillis;
    private final long timeoutMillis;
    private final String healthPath;

    public HealthChecker(List<BackendInstance> backends, long intervalMillis, long timeoutMillis, String healthPath) {
        this.backends = backends;
        this.intervalMillis = intervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.healthPath = healthPath;
        for (BackendInstance backend : backends) {
            healthMap.put(backend.getId(), HealthStatus.UNKNOWN);
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAll, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public HealthStatus getHealth(String backendId) {
        return healthMap.getOrDefault(backendId, HealthStatus.UNKNOWN);
    }

    public Map<String, HealthStatus> getAllHealth() {
        return healthMap;
    }

    public void forceAllHealthy() {
        for (BackendInstance backend : backends) {
            healthMap.put(backend.getId(), HealthStatus.HEALTHY);
        }
    }

    private void checkAll() {
        for (BackendInstance backend : backends) {
            HealthStatus status = ping(backend) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
            healthMap.put(backend.getId(), status);
        }
    }

    private boolean ping(BackendInstance backend) {
        String url = backend.getBaseUrl() + healthPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMillis))
                .GET()
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
