# Deep Dive #1: End-to-End Non-Blocking Filter Chain

**Question:** Can the Gateway's WebFilter chain (JWT validation → rate limiting → circuit breaker → load balancing → forward) be composed so it never blocks the event-loop, and correctly short-circuits on failure at each step?

**Verdict:** Yes, with three concrete rules that must be enforced consistently across every filter.

---

## Rule 1 — Every I/O call in the chain must return a reactive type

`JwtValidator.validate(...)` returns `Mono<AuthenticatedUser>`, not `AuthenticatedUser`. `ReactiveStringRedisTemplate.execute(...)` returns `Flux<Long>`. There is no point in the chain where a `.block()` call is acceptable — including in tests, where `StepVerifier` is used instead of blocking on the result.

**Enforcement mechanism:** BlockHound (see `JwtValidationWebFilterTest`). Code review alone won't reliably catch this — a blocking call buried inside a third-party library dependency is invisible until it's instrumented at runtime. BlockHound should be wired into the test suite from day one, not added later once a performance problem is already in production.

## Rule 2 — Early-return means writing the response AND calling `setComplete()`, and never calling `chain.filter()` afterward

Both example filters follow the same shape on the failure path:
```java
exchange.getResponse().setStatusCode(...);
return exchange.getResponse().setComplete();
```
Forgetting `setComplete()` is the most common bug here — the status code is set but the response is never actually committed, and the client hangs. The second `JwtValidationWebFilterTest` test makes this failure mode explicit by asserting `chain.filter()` is never invoked on the failure path — this is the kind of bug that a manual smoke test might not catch (a hung connection under low load can look like a slow response, not a bug).

## Rule 3 — Atomicity for check-then-act patterns must be pushed into Redis itself, not composed client-side

The rate limiter could have been written as two separate reactive calls — `GET` the counter, check it, then `INCR` it — but that reintroduces the exact race condition already discovered once in this project (the same-second JWT generation collision that led to the opaque refresh token redesign in `service-auth`). The Lua script (`TOKEN_BUCKET_SCRIPT`) makes `INCR` + `EXPIRE` + the threshold check atomic on the Redis server side, so no amount of Reactor-side concurrency can produce a race — the non-blocking chain doesn't need to compensate for it.

---

## What this deep dive does NOT resolve (left for later steps)

- **How `chain.filter()` composes with the circuit breaker and load balancer filters** (deep dive #2 and #3) — this prototype only proves the pattern for the first two filters in the chain; the same rules apply to the remaining ones but haven't been prototyped yet.

## Resolved after this deep dive

- **Fail-open vs. fail-closed when Redis itself is unreachable.** Decided: fail open (see ADR-008). Rate limiting is an abuse/overload safeguard, not the system's security boundary — JWT validation (independent of Redis) already handles that — so a Redis outage should degrade rate limiting, not take the whole Gateway down as a single point of failure. `RateLimitingWebFilter` now has an explicit `onErrorResume` that lets requests through and records a `failopen` metric so the degradation stays observable.
