package com.apigateway.circuit;

import com.apigateway.model.CircuitState;

public interface CircuitBreaker {
    boolean allowRequest();
    void recordSuccess();
    void recordFailure();
    CircuitState getState();
    void reset();
}
