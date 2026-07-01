# ADR-008: Rate Limiter Fail-Open Behavior on Redis Unavailability

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

`RateLimitingWebFilter` (deep dive #1) depends on Redis to enforce the token-bucket rate limit. If Redis becomes unreachable — network partition, Redis container down, connection pool exhaustion — the filter has two possible behaviors:

- **Fail-closed:** reject all requests (or all requests for the affected check) while Redis is unavailable, since the rate limit cannot be evaluated.
- **Fail-open:** let requests through without rate limiting while Redis is unavailable, and restore enforcement once Redis recovers.

Fail-closed effectively makes Redis a **single point of failure (SPOF)** for the entire Gateway: an outage in a component that exists purely to prevent abuse would take down all legitimate traffic too, including traffic that was never going to hit the rate limit in the first place.

Critically, rate limiting is not the system's security boundary. Authentication and authorization are enforced by JWT signature validation (`JwtValidationWebFilter`), which has no dependency on Redis. Rate limiting exists to protect against abuse/overload, not to authenticate or authorize requests. Losing rate limiting temporarily does not create an authentication or authorization gap — a request that would have been rejected as unauthenticated is still rejected, Redis or not.

## Decision

**The rate limiter fails open.** If the Redis call in `RateLimitingWebFilter` errors (connection failure, timeout), the filter logs/records the failure and allows the request to proceed to the next filter in the chain, rather than rejecting it.

## Consequences

**Positive:**
- Redis is no longer a SPOF for the Gateway as a whole. A Redis outage degrades one specific protection (abuse/overload prevention) without taking down the system's actual security boundary (authentication, enforced independently via JWT validation).
- Matches the actual risk being managed: rate limiting protects capacity and fairness, not confidentiality or integrity — a temporary lapse in enforcement is an availability/cost concern, not a security breach.

**Negative / Accepted limitations:**
- During a Redis outage, the system is temporarily exposed to abuse it would normally rate-limit (e.g., a single client hammering the Gateway with requests). This is an accepted tradeoff given that Redis outages are expected to be rare and short, and the alternative (fail-closed) causes guaranteed full-system unavailability on every Redis blip, which is a worse outcome for a case whose actual harm (security) does not apply here.
- This failure must be observable: every fail-open event should increment a dedicated metric (e.g., `gateway.ratelimit.failopen.count`) so it's visible in Grafana (see ADR-006) rather than silently degrading protection with no signal.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Fail-closed (reject all requests when Redis is down) | Turns Redis into a SPOF for the entire Gateway; an outage in an abuse-prevention component should not equal total system unavailability, especially when the actual security boundary (JWT validation) is unaffected |
| Circuit-breaker around the Redis call itself, fail-closed once open | Adds complexity without changing the core tradeoff — still ties overall availability to Redis's availability, just with an extra layer before the SPOF behavior kicks in |

## Revisit Triggers

This decision should be revisited if:
- Rate limiting is ever asked to enforce something with actual security consequences (e.g., brute-force login attempt throttling on a sensitive endpoint), in which case fail-open may not be acceptable for that specific route and a route-level override may be needed.
- Redis outages become frequent enough that fail-open's abuse-exposure window becomes a recurring operational problem rather than a rare edge case.
