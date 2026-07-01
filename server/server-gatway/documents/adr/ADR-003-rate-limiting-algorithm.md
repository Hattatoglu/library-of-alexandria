# ADR-003: Rate Limiting Algorithm

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

The Gateway must limit the number of requests a client can make within a given time period (FR-02). Rate limit state needs to be shared across multiple Gateway instances, so it is stored in Redis rather than in-memory.

Two common algorithms were considered:

- **Token bucket:** Each client has a bucket with a fixed capacity that refills over time. A request consumes a token; if the bucket is empty, the request is rejected. Allows short bursts up to the bucket capacity while enforcing a long-term average rate. Implementable in Redis with a simple `INCR` + `EXPIRE` pattern.
- **Sliding window:** Tracks requests within a moving time window for more precise rate enforcement, avoiding the "double burst at window boundary" problem inherent to fixed windows. Typically requires a Redis sorted set (`ZADD`/`ZREMRANGEBYSCORE`) to track individual request timestamps, which is more complex to implement and reason about.

## Decision

**Token bucket** is used as the rate limiting algorithm, implemented in Redis using `ReactiveRedisTemplate` for non-blocking access.

## Consequences

**Positive:**
- Simple to implement and reason about with basic Redis operations (`INCR` + `EXPIRE`), fitting the reactive, non-blocking design of the Gateway.
- Tolerates short bursts of legitimate traffic (e.g., a client loading multiple resources at once) without rejecting requests, while still enforcing a long-term average rate.
- Sufficient precision for this system's current scale and traffic patterns.

**Negative / Accepted limitations:**
- Less precise than a sliding window at the boundary between refill intervals — a client could theoretically send close to double the intended rate across a boundary. This is accepted as an acceptable tradeoff for implementation simplicity at this scale.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Sliding window (sorted-set based) | More precise but requires more complex Redis data structures and query patterns; the added precision is not needed at this system's traffic volume |
| Fixed window counter | Simpler than sliding window but has a well-known boundary problem (up to 2x burst at window edges) that token bucket avoids while remaining similarly simple to implement |

## Open Parameters (not architectural, to be set as configuration)

- Bucket capacity (max burst size)
- Refill rate (tokens per time unit)
- Rate limiting key: per-user (JWT subject) with IP-based fallback for unauthenticated requests, per FR2

## Revisit Triggers

This decision should be revisited if:
- Traffic patterns show the token bucket's boundary imprecision causing real issues (e.g., clients regularly exceeding intended limits by exploiting refill timing).
- The system needs more granular, per-second-accurate rate enforcement for a specific endpoint (e.g., a sensitive write operation).
