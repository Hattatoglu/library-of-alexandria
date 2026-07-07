# ADR-007: Circuit Breaker and Load Balancer Ordering

**Status:** Accepted (with a defined future change)
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

The Gateway's request flow (see `hld-gateway-request-flow.mermaid`) needs to decide, for each request, whether to check circuit breaker state before or after selecting a downstream instance via the load balancer. This ordering depends on the **granularity** at which the circuit breaker operates:

- **Service-level circuit breaker:** one breaker per downstream service (e.g., one for Catalog, one for Loan), independent of how many instances that service has.
- **Instance-level circuit breaker:** one breaker per individual instance (e.g., a separate breaker for `catalog-1` and `catalog-2`).

Currently, each downstream service (Catalog, Loan) runs as a **single instance**. In this state, a service-level breaker and an instance-level breaker are equivalent — there's only one instance to break on. This makes the ordering question moot for the current deployment, but the decision still needs to be made explicit because it will matter as soon as multiple instances are introduced.

## Decision

**Current state (single instance per service):** Circuit breaker check happens **before** load balancer instance selection. If the service-level circuit is open, the request fails fast without ever reaching the load balancer step — since there is only one instance, this is equivalent to failing fast on that instance.

**Future state (multiple instances per service):** When a downstream service has multiple instances, the ordering **inverts**: the load balancer selects an instance **first**, then the circuit breaker check is evaluated **per instance**, not per service. Rationale: with multiple instances, one instance failing should not open a service-wide circuit and block traffic to healthy instances of the same service. Each instance needs its own breaker so that a failing `catalog-1` doesn't cause requests to a healthy `catalog-2` to be rejected.

## Consequences

**Positive:**
- The current single-instance implementation stays simple (one breaker per service, checked early, cheap fail-fast).
- The future change is anticipated and documented now, rather than being discovered as a surprise refactor when multi-instance support (ADR-005) is actually implemented.

**Negative / Accepted limitations:**
- This is a **known, planned breaking change** to the Gateway's internal filter ordering and to the circuit breaker's granularity (service-level → instance-level). It is not implemented yet — it is documented here so it isn't lost or re-litigated when the load balancer in ADR-005 moves from a single static instance to multiple instances.
- Until multi-instance support is implemented, `hld-gateway-request-flow.mermaid` correctly reflects only the current (single-instance) ordering. It should be redrawn when this change is implemented.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Keep service-level circuit breaker even after multi-instance support | Would cause one failing instance to block traffic to healthy sibling instances of the same service — defeats part of the purpose of having multiple instances |
| Implement instance-level circuit breaker now, even with a single instance | Unnecessary complexity today; service-level and instance-level are equivalent with one instance, so there is nothing to gain by building the more complex version early |

## Revisit Triggers

This decision must be acted on when:
- A downstream service (Catalog or Loan) is deployed with more than one instance, per ADR-005. At that point: (1) introduce per-instance circuit breakers, (2) reorder the Gateway filter chain so load balancer instance selection happens before the circuit breaker check, (3) update `hld-gateway-request-flow.mermaid` to reflect the new ordering.
