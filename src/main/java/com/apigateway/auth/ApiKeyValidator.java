package com.apigateway.auth;

public interface ApiKeyValidator {
    boolean isValid(String apiKey);
}
