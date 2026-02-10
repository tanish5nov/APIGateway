package com.apigateway.circuit;

import com.apigateway.model.CircuitState;

public class DefaultCircuitBreaker implements CircuitBreaker {
    private final int failureThreshold;
    private final long resetTimeoutMillis;

    private CircuitState state = CircuitState.CLOSED;
    private int failureCount = 0;
    private long lastFailureTime = 0L;
    private boolean halfOpenInFlight = false;

    public DefaultCircuitBreaker(int failureThreshold, long resetTimeoutMillis) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMillis = resetTimeoutMillis;
    }

    @Override
    public synchronized boolean allowRequest() {
        if (state == CircuitState.OPEN) {
            long now = System.currentTimeMillis();
            if (now - lastFailureTime >= resetTimeoutMillis) {
                state = CircuitState.HALF_OPEN;
            } else {
                return false;
            }
        }

        if (state == CircuitState.HALF_OPEN) {
            if (halfOpenInFlight) {
                return false;
            }
            halfOpenInFlight = true;
            return true;
        }

        return true;
    }

    @Override
    public synchronized void recordSuccess() {
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.CLOSED;
            failureCount = 0;
            halfOpenInFlight = false;
            return;
        }
        failureCount = 0;
    }

    @Override
    public synchronized void recordFailure() {
        lastFailureTime = System.currentTimeMillis();
        if (state == CircuitState.HALF_OPEN) {
            state = CircuitState.OPEN;
            failureCount = failureThreshold;
            halfOpenInFlight = false;
            return;
        }

        failureCount++;
        if (failureCount >= failureThreshold) {
            state = CircuitState.OPEN;
        }
    }

    @Override
    public synchronized CircuitState getState() {
        return state;
    }

    @Override
    public synchronized void reset() {
        state = CircuitState.CLOSED;
        failureCount = 0;
        lastFailureTime = 0L;
        halfOpenInFlight = false;
    }
}
