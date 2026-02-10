package com.apigateway.lb;

import com.apigateway.model.BackendInstance;

import java.util.Optional;

public interface LoadBalancer {
    Optional<BackendInstance> selectBackend();
}
