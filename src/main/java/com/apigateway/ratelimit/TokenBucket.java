package com.apigateway.ratelimit;

public class TokenBucket {
    private final int capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillMillis;

    public TokenBucket(int capacity, double refillPerSecond, long nowMillis) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefillMillis = nowMillis;
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - lastRefillMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        double refill = (elapsedMillis / 1000.0) * refillPerSecond;
        if (refill > 0) {
            tokens = Math.min(capacity, tokens + refill);
            lastRefillMillis = now;
        }
    }
}
