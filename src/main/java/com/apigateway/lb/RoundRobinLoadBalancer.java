package com.apigateway.lb;

import com.apigateway.health.HealthChecker;
import com.apigateway.model.BackendInstance;
import com.apigateway.model.HealthStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final List<BackendInstance> backends;
    private final HealthChecker healthChecker;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinLoadBalancer(List<BackendInstance> backends, HealthChecker healthChecker) {
        this.backends = backends;
        this.healthChecker = healthChecker;
    }

    @Override
    public Optional<BackendInstance> selectBackend() {
        if (backends.isEmpty()) {
            return Optional.empty();
        }

        int size = backends.size();
        int start = Math.abs(index.getAndIncrement());
        for (int i = 0; i < size; i++) {
            int pos = (start + i) % size;
            BackendInstance backend = backends.get(pos);
            HealthStatus status = healthChecker.getHealth(backend.getId());
            if (status != HealthStatus.UNHEALTHY) {
                return Optional.of(backend);
            }
        }
        return Optional.empty();
    }
}
