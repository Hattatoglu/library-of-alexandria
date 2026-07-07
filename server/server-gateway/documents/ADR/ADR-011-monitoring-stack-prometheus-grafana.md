# ADR-006: Monitoring Stack — Prometheus and Grafana on Docker Compose

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway, all services

---

## Context

The system's functional requirements (rate limiting, circuit breaking, routing) all need observability: rate limit rejection counts, circuit breaker state transitions, request latency, and similar metrics need to be visible for both operational monitoring and system-design demonstration purposes.

The project already has a Micrometer-based metrics foundation: `service-auth` exposes 18 counters, 4 timers, and 1 distribution summary via a dedicated `AuthMetrics` component, using Spring Boot Actuator with the Micrometer Prometheus registry dependency. This established pattern needs to be extended to the Gateway and made consumable as dashboards, not just raw metrics endpoints.

The deployment target is Docker Compose (see ADR-005), which constrains the options to tools that run well as containers within a Compose network without requiring external managed infrastructure.

## Decision

**Prometheus and Grafana are added as services in `docker-compose.yml`.** Each Spring Boot service — including the Gateway — exposes its metrics via the existing Micrometer/Actuator setup at `/actuator/prometheus`. The `prometheus` container is configured (via `prometheus.yml`) to scrape these endpoints using Docker Compose service names as scrape targets (e.g., `service-auth:8080`, `gateway:8080`). The `grafana` container uses Prometheus as its data source for dashboards.

## Consequences

**Positive:**
- Builds directly on the existing Micrometer/Actuator investment already made in `service-auth` — no new metrics library or instrumentation approach is introduced.
- Runs entirely within the existing Docker Compose environment; no external monitoring service or account is required.
- Docker Compose's internal DNS (service names) is sufficient for Prometheus scrape target configuration, since this is a static, known set of services (unlike the per-request load-balancing case in ADR-005, which is why the DNS-caching concern from ADR-005 does not apply here — Prometheus performs its own periodic scrape, it does not rely on a cached long-lived connection).
- Gateway-specific metrics (rate limit rejections, circuit breaker state transitions) can be added to the same `/actuator/prometheus` endpoint pattern already used elsewhere in the project, keeping instrumentation consistent across services.

**Negative / Accepted limitations:**
- Prometheus and Grafana data are not persisted beyond the container lifecycle unless volumes are explicitly configured — acceptable for local development and portfolio demonstration, but would need revisiting for any long-term metrics retention need.
- No alerting (e.g., Alertmanager) is included in this decision — dashboards are for visibility, not automated incident response. This can be added later without affecting this decision.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Managed/cloud monitoring service (e.g., Datadog, Grafana Cloud) | Adds external dependency and cost inappropriate for a self-contained Docker Compose portfolio project |
| Logging-only observability (no metrics dashboard) | Insufficient for demonstrating the circuit breaker/rate limiter behavior visually, which is a stated goal alongside the functional implementation |

## Revisit Triggers

This decision should be revisited if:
- The deployment target moves to Kubernetes, where a Prometheus Operator / kube-prometheus-stack pattern would likely replace the manually configured Docker Compose setup.
- Long-term metrics retention or alerting becomes a requirement.
