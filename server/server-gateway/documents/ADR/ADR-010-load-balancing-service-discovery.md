# ADR-005: Load Balancing and Service Discovery

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

When a downstream service (Catalog, Loan) runs multiple instances, the Gateway must distribute requests across them (FR-05). Two decisions were needed: how the Gateway discovers available instances, and how it chooses among them per request.

**Deployment target:** The system runs on **Docker Compose**, not Kubernetes. This rules out Kubernetes Service (a Kubernetes-native construct) as a discovery mechanism.

**Discovery options considered for Docker Compose:**
- Docker's embedded DNS: services on the same Compose network can resolve each other by service name, and scaling a service to multiple containers under the same name can produce round-robin behavior at DNS resolution time.
- Static configuration: instance addresses listed explicitly in application config.
- A dedicated service discovery tool (Eureka, Consul).

Relying on Docker's embedded DNS round-robin was considered but has a practical problem: Java HTTP clients (including the reactive `WebClient` used by the Gateway) typically cache DNS resolution results for some duration, so repeated requests may keep hitting the same resolved IP instead of benefiting from DNS-level round-robin on each lookup. This makes DNS-based discovery an unreliable load-balancing mechanism in practice, not just a discovery mechanism.

Beyond the technical reliability concern, relying on Docker's DNS round-robin would mean the Gateway itself implements no load-balancing logic — the platform silently handles it. Since implementing and being able to explain load-balancing logic is itself a goal of this project (used as system design interview practice), delegating it to the platform was also considered undesirable independent of the technical risk.

Introducing a dedicated discovery service (Eureka/Consul) was considered disproportionate for the project's current scale — it adds another moving part to operate and reason about for a small number of downstream services with a known, fixed set of instances.

## Decision

**Static configuration** is used for service discovery: downstream instance addresses (e.g., `catalog-1:8080`, `catalog-2:8080`) are listed explicitly in the Gateway's application configuration. The Gateway implements its own **round robin** distribution logic to select an instance per request from that static list.

## Consequences

**Positive:**
- No dependency on DNS-caching behavior or platform-level load-balancing — behavior is fully controlled and observable within the Gateway's own code.
- No additional infrastructure component (no Eureka/Consul to run and operate) — appropriate for the current scale.
- The Gateway now contains an explicit, testable load-balancing implementation, which supports the project's system-design learning goal.

**Negative / Accepted limitations:**
- Static configuration does not automatically adapt when instances are added or removed (e.g., during scaling) — configuration must be updated manually. This is acceptable for a Docker Compose environment with a small, manually-managed set of instances, but would not scale to a dynamic, auto-scaling environment.
- If the deployment target later moves to Kubernetes, this decision should be revisited — Kubernetes Service already provides load-balanced discovery natively, making the Gateway's own static-config round robin redundant in that environment.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Docker embedded DNS round-robin | Unreliable in practice due to DNS caching in Java HTTP clients; also delegates load-balancing logic to the platform instead of the Gateway, which works against this project's learning goal |
| Kubernetes Service | Not applicable — deployment target is Docker Compose, not Kubernetes |
| Dedicated discovery service (Eureka, Consul) | Disproportionate operational complexity for the current number of services and instances |

## Revisit Triggers

This decision should be revisited if:
- The deployment target moves from Docker Compose to Kubernetes (Kubernetes Service would replace this mechanism entirely).
- The number of downstream instances grows large enough, or changes frequently enough, that manually maintaining static configuration becomes impractical.
