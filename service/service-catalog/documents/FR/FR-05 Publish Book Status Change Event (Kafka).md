# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-05 Publish Book Status Change Event (Kafka)

**Definition:** Whenever a book's base status changes (via FR-01 Add, FR-02 Remove, or FR-04 Change Status), service-catalog publishes a `BookStatusChangedEvent` so other services can react.

- **Topic:** a single topic, `book-events`, carries all event types related to a book — not one topic per event type (see ADR-007).
- **Partition key:** `bookId` — guarantees all events for a given book are delivered to consumers in the order they were produced.
- **Event discriminator:** each message includes an `eventType` field. Currently the only value produced is `STATUS_CHANGED`; the schema is designed to accommodate additional event types later without a topic-design change.
- **Event payload (minimum):** `bookId`, `eventType`, `oldStatus` (nullable — absent on initial creation), `newStatus`, timestamp.
- **Delivery guarantee:** best-effort, post-commit publish (see ADR-008). The database write (status change) and the Kafka publish are **not atomic** — a publish failure after a successful DB commit results in a silently lost event under the current (interim) design. This is a known, accepted limitation, not an oversight (see ADR-008 for the planned Transactional Outbox migration).
- **Consumers:** none exist yet within the current scope. `service-loan` is the anticipated first consumer (not built as part of this component); `service-notifier` is a further anticipated future consumer.

**Out of scope:**
- Any consumer-side logic (service-loan's or service-notifier's handling of these events) — entirely out of scope for service-catalog.
- Transactional Outbox implementation — deferred per ADR-008; this FR describes the interim (simple, non-atomic) publish behavior only.
- Event schema versioning/evolution strategy (e.g., Schema Registry) — not addressed yet; to be revisited if/when event schemas need to evolve in a backward-incompatible way.
