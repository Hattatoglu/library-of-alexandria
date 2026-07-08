# ADR-017: Blocking Web Stack (Spring MVC)

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

`server-gateway` uses Spring WebFlux (reactive), justified by its role as a proxy handling high-concurrency I/O-bound work (JWT validation, rate limiting, forwarding) with minimal CPU-bound logic per request. `service-auth` uses Spring MVC (blocking), justified by its CPU/business-logic-heavy, DB-heavy nature.

`service-catalog` is architecturally closer to `service-auth` than to the Gateway: it performs CRUD operations against PostgreSQL, enforces domain validation (state machine transitions), and — unlike the Gateway — is not a high-concurrency proxy sitting in front of every request in the system. Its request volume is bounded by actual catalog management activity (adding/removing/updating books), not by being on the critical path of every user-facing request.

## Decision

**`service-catalog` uses Spring MVC (blocking)**, consistent with `service-auth`'s stack choice, rather than WebFlux.

## Consequences

**Positive:**
- Consistent with `service-auth`'s established stack — shared JDBC/JPA patterns, shared testing approach (Testcontainers, Cucumber BDD with `cucumber-spring`), no need to reason about reactive-specific pitfalls (blocking-call detection, Reactor Context propagation) that were a recurring source of debugging effort in `server-gateway`.
- Kafka producer integration (Spring Kafka) has long-standing, well-documented blocking-style support; no need to adopt reactive Kafka clients for a service with no reactive-specific performance requirement.
- Simpler mental model for a service whose logic is CRUD + state machine validation, not I/O orchestration.

**Negative / Accepted limitations:**
- Thread-per-request model does not scale to the same concurrency levels as WebFlux under extreme load — accepted as irrelevant at this system's scale, and consistent with the same tradeoff already accepted for `service-auth`.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| WebFlux (matching server-gateway) | `service-catalog`'s workload (CRUD + domain validation, not high-concurrency proxying) doesn't match the profile that justified WebFlux for the Gateway; would introduce reactive-specific complexity (as seen repeatedly during `server-gateway` development) without a corresponding benefit here |

## Revisit Triggers

This decision should be revisited if `service-catalog` is ever put directly on a high-concurrency, latency-sensitive critical path comparable to the Gateway's — not expected given its role as a catalog-management service behind the Gateway.
