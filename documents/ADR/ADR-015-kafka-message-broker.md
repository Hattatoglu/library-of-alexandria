# ADR-001: Introduction of Kafka as the System's Message Broker

**Status:** Accepted
**Date:** 2026-07-08
**Component:** System-wide (introduced with service-catalog, consumed by service-loan)

---

## Context

`service-catalog` needs to notify other services when a book's base bookStatus changes (AVAILABLE, MAINTENANCE, LOST, REMOVED). The immediate consumer is `service-loan`, which needs to know a book's catalog-level bookStatus to correctly present it to users (e.g., a book in MAINTENANCE should not appear as borrowable). A second consumer is planned but not yet built: `service-notifier`, which would notify users when a book they're interested in becomes available again.

This is the first cross-service, asynchronous integration point in the system. Prior services (`service-auth`, `server-gateway`) communicate synchronously (REST) or not at all. Introducing a message broker is a system-wide infrastructure decision, not specific to `service-catalog` alone — every future service that needs to react to catalog state changes (starting with `service-loan`, later `service-notifier`) will depend on this same broker and topic convention.

Kafka was the message broker discussed and assumed throughout the requirements/HLD process for this feature (event publishing on bookStatus change, consumed by `service-loan`).

## Decision

**Kafka is adopted as the system's message broker** for asynchronous, event-driven communication between services. `service-catalog` is the first producer; `service-loan` will be the first consumer (implemented later, out of scope for the current `service-catalog` work).

Deployment/infrastructure setup (Docker Compose configuration for Kafka) is handled directly by the project owner, not addressed in this ADR.

## Consequences

**Positive:**
- Decouples `service-catalog` from needing to know which services care about bookStatus changes, or how many — new consumers (like `service-notifier` later) can be added without any change to `service-catalog`.
- Matches the explicit non-goal already established for `service-catalog`: no synchronous dependency on `service-loan` (see `service-catalog` ADR-003). Kafka is what makes that boundary possible while still keeping `service-loan` informed.
- Establishes a reusable pattern (topic + event schema conventions) for future producers/consumers in the system, rather than each service inventing its own integration style.

**Negative / Accepted limitations:**
- Introduces a new piece of infrastructure to operate, monitor, and reason about (an addition to the growing Docker Compose stack: Redis, Prometheus, Grafana, and now Kafka).
- Eventual consistency is now a property of the system: `service-loan`'s view of a book's bookStatus can lag behind `service-catalog`'s by however long event delivery + consumption takes. This is accepted as appropriate for this use case (bookStatus changes are not expected to require sub-second cross-service consistency).
- Topic design, event schema format, and producer delivery guarantees are `service-catalog`-specific decisions, addressed separately (see `service-catalog` ADR-007 and ADR-008) rather than mandated system-wide by this ADR — future producers may make different choices as their needs differ.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Synchronous REST call from service-catalog to service-loan on bookStatus change | Violates the explicit architectural boundary already decided (no synchronous dependency on service-loan) — would also couple service-catalog's availability to service-loan's, and to every future consumer's, uptime |
| Other message brokers (RabbitMQ, etc.) | Not evaluated in depth — Kafka was the working assumption throughout requirements/HLD discussion and fits the "multiple current/future consumers reading an event stream" shape of this problem well |

## Revisit Triggers

This decision should be revisited if:
- The number of services needing event-driven integration turns out to be very small (e.g., only ever `service-loan`), in which case a lighter-weight mechanism might have sufficed — unlikely given `service-notifier` is already planned.
- Kafka's operational overhead proves disproportionate to the project's actual scale (a portfolio/interview-preparation system) — if so, this should be an explicit, revisited decision, not a silent workaround.
