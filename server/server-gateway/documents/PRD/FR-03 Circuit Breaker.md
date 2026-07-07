# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-03 Circuit Breaker

**Definition:** When a downstream service returns errors or responds slowly, the Gateway temporarily stops sending requests to that service.

- The circuit breaker operates **per downstream service** (separate breaker for Catalog, separate for Loan — one opening does not affect the other).
- When the circuit is `OPEN`, requests are not sent to the affected service; an error response is returned immediately instead.
- A half-open retry interval defines when the circuit attempts to close again.
- **Opening condition: count-based.** The circuit opens when a configurable number of the last N requests to a given downstream service fail (e.g., X out of the last 10). Chosen over a time-based (rolling error-rate) window because, at this system's traffic volume, "how many requests occurred in the last X seconds" is unpredictable — a fixed request-count window gives more predictable, testable behavior. Exact N and failure threshold are configuration parameters, to be finalized in an ADR.
- Library choice: *TBD — to be finalized in an ADR (candidate: Resilience4j).*