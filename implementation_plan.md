# Implementation Plan (Granular)

## 0. Repository Discovery
- List repository files and folders.
- Identify build tool (Maven/Gradle) and Java version.
- Note existing code conventions and package structure.
- Confirm how the service is intended to run (CLI, server, tests).

## 1. Project Skeleton
- Create base package structure:
  - `gateway.core` for request flow orchestration
  - `gateway.auth` for API key validation
  - `gateway.ratelimit` for token bucket and sliding window
  - `gateway.circuit` for circuit breaker
  - `gateway.lb` for load balancer
  - `gateway.health` for health checks
  - `gateway.logging` for request logging
  - `gateway.config` for configuration models
  - `gateway.model` for shared data structures
- Add a main entry point class (e.g., `GatewayServer`).

## 2. Core Models
- Define `RequestContext` (client ID, API key, path, method, timestamp).
- Define `GatewayResponse` (status, headers, body).
- Define `BackendInstance` (id, host, port, metadata).
- Define `HealthStatus` enum (HEALTHY, UNHEALTHY, UNKNOWN).
- Define `CircuitState` enum (CLOSED, OPEN, HALF_OPEN).

## 3. Configuration
- Create `GatewayConfig` with:
  - API keys list or map
  - Rate limit settings (per client, per API key)
  - Circuit breaker settings (failure threshold, reset timeout)
  - Backend list
  - Health check interval and timeout
- Implement config loading:
  - Start with hardcoded defaults
  - Add file-based config (YAML/JSON/properties) if needed

## 4. API Key Authentication
- Create `ApiKeyValidator` interface.
- Implement `StaticApiKeyValidator` backed by config.
- Add failure response for missing or invalid keys.

## 5. Rate Limiting
- Create `RateLimiter` interface with `allow(RequestContext)`.
- Token Bucket:
  - `TokenBucket` model (capacity, refill rate, current tokens, last refill time)
  - `TokenBucketLimiter` that refills on each request
- Sliding Window:
  - `SlidingWindowLimiter` using timestamps queue per client
  - Remove old timestamps outside window
- `RateLimiterFactory` to choose strategy from config.

## 6. Circuit Breaker
- Create `CircuitBreaker` interface with `allowRequest()` and `recordSuccess/Failure()`.
- Implement `DefaultCircuitBreaker`:
  - Track failure count and last failure time
  - Transition rules:
    - CLOSED -> OPEN when failures exceed threshold
    - OPEN -> HALF_OPEN after timeout
    - HALF_OPEN -> CLOSED on success or OPEN on failure
- Maintain circuit breaker per backend instance.

## 7. Health Checks
- Create `HealthChecker` that pings backends at fixed intervals.
- Maintain a health registry (`Map<BackendId, HealthStatus>`).
- Mark unhealthy on repeated failures; recover on success.
- Expose query method for load balancer.

## 8. Load Balancer
- Create `LoadBalancer` interface with `selectBackend()`.
- Implement `RoundRobinLoadBalancer`:
  - Keep atomic index
  - Skip unhealthy backends
  - Return error if none healthy
- Integrate health registry to filter backends.

## 9. Request Logging
- Create `RequestLogger` interface.
- Implement `ConsoleRequestLogger` or file-based logger.
- Log request start, selected backend, and outcome.

## 10. Request Flow Orchestration
- Build `GatewayHandler`:
  - Parse inbound request into `RequestContext`
  - Validate API key
  - Apply rate limiter
  - Select backend via load balancer
  - Check circuit breaker for backend
  - Forward request to backend
  - Record success/failure for circuit breaker
  - Return response

## 11. Backend Forwarding
- Implement a basic HTTP client adapter for forwarding requests.
- Map backend responses back to `GatewayResponse`.
- Handle timeouts and exceptions.

## 12. Error Handling
- Standardize error responses:
  - 401 for invalid API key
  - 429 for rate limit exceeded
  - 503 for no healthy backends / circuit open
  - 502 for backend errors

## 13. Basic UI (Just Working)
- Add a simple admin endpoint group:
  - `GET /admin/health`
  - `GET /admin/backends`
  - `GET /admin/circuit`
  - `GET /admin/ratelimits`
  - `GET /admin/logs`
- Create a basic static HTML page:
  - Fetch data from `/admin/*`
  - Render plain tables/lists
  - No styling beyond basic layout
- Serve the UI from the backend (static files).

## 14. Tests
- Unit tests for:
  - Token bucket limiter
  - Sliding window limiter
  - Circuit breaker transitions
  - Round-robin selection
- Integration test (happy path):
  - Simulate request flow with mock backend

## 15. Example Usage
- Add a small sample config and example run command.
- Document how to start the gateway and send a sample request.

## 16. Final Pass
- Run tests and fix failures.
- Review README for accuracy and add pointers to config and usage.
