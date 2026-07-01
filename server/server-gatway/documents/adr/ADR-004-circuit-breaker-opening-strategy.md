# ADR-004: Circuit Breaker Opening Strategy

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

The Gateway needs to stop forwarding requests to a downstream service (Catalog, Loan) when that service is failing or responding too slowly, to avoid cascading failures and wasted requests (FR-03). Two strategies for deciding when to open the circuit were considered:

- **Count-based:** The circuit opens when a configurable number of the last N requests to a service fail (e.g., 5 out of the last 10).
- **Time-based:** The circuit opens based on the error rate observed within a rolling time window (e.g., 50% error rate over the last 30 seconds).

Time-based windows work well under steady, predictable traffic, where "how many requests occurred in the last X seconds" is a meaningful and stable quantity. At this system's current traffic volume, that assumption does not reliably hold — a low-traffic period could see very few requests in a time window, making the error-rate calculation noisy or slow to react (e.g., a single failure out of two requests in the window looks identical in severity to a single failure out of fifty).

## Decision

**Count-based** opening is used: the circuit breaker (per downstream service — Catalog and Loan each have independent breaker instances) opens when a configurable number of the last N requests fail.

## Consequences

**Positive:**
- Behavior is predictable and testable regardless of request rate — "N requests, X failures" is a fixed, deterministic condition rather than one that depends on how much traffic happened to arrive in a given time window.
- Easier to reason about and to write deterministic tests for (a known sequence of N requests with a known number of failures), which fits the project's existing testing approach (fake adapters, explicit scenarios).

**Negative / Accepted limitations:**
- Under very low traffic, it may take a long time to accumulate N requests, delaying detection of a failing downstream service. This is an accepted tradeoff at the current scale; it would need to be revisited if traffic volume increases significantly or if faster failure detection becomes a requirement regardless of request volume.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Time-based (rolling error rate) | Unreliable at this system's current low/variable traffic volume — the "requests per window" denominator is too unstable to produce meaningful error-rate calculations |

## Open Parameters (not architectural, to be set as configuration)

- Sliding window size (N — number of requests evaluated)
- Failure threshold (X — number of failures within N that trips the circuit)
- Half-open retry interval (how long the circuit stays open before allowing a trial request)
- Library: Resilience4j is the current candidate; final selection to be confirmed separately (see functional requirements doc, open item #6)

## Revisit Triggers

This decision should be revisited if:
- Traffic volume increases enough that a time-based error-rate window becomes statistically stable and reliable.
- Detection latency under count-based evaluation (waiting for N requests) proves too slow for a specific downstream dependency's failure characteristics.
