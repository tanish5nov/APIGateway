package com.apigateway.ratelimit;

import com.apigateway.model.RequestContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketLimiter implements RateLimiter {
    private final int capacity;
    private final double refillPerSecond;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketLimiter(int capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    @Override
    public boolean allow(RequestContext context) {
        String key = context.getApiKey();
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond, System.currentTimeMillis()));
        return bucket.tryConsume();
    }
}
