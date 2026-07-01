# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-XX List

1. FR-01 Token Validation & Routing

2. FR-02 Rate Limiting

3. FR-03 Circuit Breaker

4. FR-04 Refresh Token Revocation (DB Delete on Logout)

5. FR-05 Load Balancing

## Non-Functional Requirements (Next Step)

This document covers functional requirements only. The following should be addressed in a separate **Non-Functional Requirements** document:

- Latency budget (upper bound on the delay added by the Gateway)
- Availability target
- Observability / metrics requirements (rate limit rejection count, circuit breaker state transitions, etc.)
- Downstream health check interval

**Deployment & monitoring stack (decided):** The system runs on **Docker Compose**. Each Spring Boot service (including the Gateway) exposes metrics via Micrometer/Actuator at `/actuator/prometheus`. A `prometheus` container scrapes these endpoints (using Docker Compose service names as scrape targets), and a `grafana` container consumes Prometheus as a data source for dashboards. Both are added as services in `docker-compose.yml`.

---

## Open Decisions (Pending ADRs)

| # | Topic | Status |
|---|------|--------|
| 1 | Rate limiting algorithm | Decided — token bucket; ADR pending |
| 2 | Circuit breaker opening condition | Decided — count-based (last N requests); library choice still open (candidate: Resilience4j) |
| 3 | Load balancing algorithm and service discovery | Decided — round robin, static config (Docker Compose); ADR pending |
| 4 | Refresh token revocation approach | Decided — DB deletion in ServiceAuth only, no Redis blacklist; ADR pending |
| 5 | Access token TTL (bounds the post-logout exposure window) | Decided — access token not revoked on logout, relies on short TTL; ADR pending |
| 6 | Circuit breaker library | To be discussed (candidate: Resilience4j) |
| 7 | Rate limit bucket capacity / refill rate values | To be discussed |
| 8 | Circuit breaker N / failure threshold values | To be discussed |

---

## Documentation Language

Starting from this document, all technical documentation, code comments, and application logs for this project will be written in **English**.
