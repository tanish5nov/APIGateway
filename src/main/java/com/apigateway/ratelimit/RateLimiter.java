package com.apigateway.ratelimit;

import com.apigateway.model.RequestContext;

public interface RateLimiter {
    boolean allow(RequestContext context);
}
