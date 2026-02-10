package com.apigateway.core;

import java.util.concurrent.atomic.AtomicLong;

public class BasicMetrics {
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong authFailures = new AtomicLong();
    private final AtomicLong rateLimitBlocked = new AtomicLong();
    private final AtomicLong circuitOpenBlocked = new AtomicLong();
    private final AtomicLong backendErrors = new AtomicLong();

    public void incTotal() {
        totalRequests.incrementAndGet();
    }

    public void incAuthFailures() {
        authFailures.incrementAndGet();
    }

    public void incRateLimitBlocked() {
        rateLimitBlocked.incrementAndGet();
    }

    public void incCircuitOpenBlocked() {
        circuitOpenBlocked.incrementAndGet();
    }

    public void incBackendErrors() {
        backendErrors.incrementAndGet();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getAuthFailures() {
        return authFailures.get();
    }

    public long getRateLimitBlocked() {
        return rateLimitBlocked.get();
    }

    public long getCircuitOpenBlocked() {
        return circuitOpenBlocked.get();
    }

    public long getBackendErrors() {
        return backendErrors.get();
    }

    public void reset() {
        totalRequests.set(0);
        authFailures.set(0);
        rateLimitBlocked.set(0);
        circuitOpenBlocked.set(0);
        backendErrors.set(0);
    }
}
