# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-05 Load Balancing

**Definition:** When a downstream service has multiple instances, the Gateway distributes requests across them.

- **Deployment target: Docker Compose** (not Kubernetes). This rules out Kubernetes Service as a discovery mechanism.
- **Service discovery mechanism: static configuration.** Downstream instance addresses (e.g., `catalog-1:8080`, `catalog-2:8080`) are listed explicitly in application config, rather than relying on Docker's embedded DNS round-robin. This is a deliberate choice: DNS-based discovery in Docker Compose is unreliable for this purpose because Java's HTTP client typically caches DNS resolutions, and it would delegate load-balancing to the platform instead of the Gateway implementing it explicitly — the latter is preferred here since implementing and explaining the LB logic is itself a goal of this project (system design interview practice).
- **Distribution algorithm: round robin.** Requests are distributed to configured instances in sequential order.
- When an instance is unhealthy, traffic is routed to the remaining healthy instances.

**Prerequisite note:** Multiple instances of downstream services are planned; load balancing is designed as part of the initial scope (not deferred).