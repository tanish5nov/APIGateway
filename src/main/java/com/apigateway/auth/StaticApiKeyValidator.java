package com.apigateway.auth;

import java.util.Set;

public class StaticApiKeyValidator implements ApiKeyValidator {
    private final Set<String> allowedKeys;

    public StaticApiKeyValidator(Set<String> allowedKeys) {
        this.allowedKeys = allowedKeys;
    }

    @Override
    public boolean isValid(String apiKey) {
        return apiKey != null && allowedKeys.contains(apiKey);
    }
}
