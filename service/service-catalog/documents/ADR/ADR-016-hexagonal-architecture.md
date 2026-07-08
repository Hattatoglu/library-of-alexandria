# ADR-016: Hexagonal Architecture

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

Two architectural styles have precedent in this project: `service-auth` uses hexagonal/ports-and-adapters architecture (Application → Domain → Persistence), while `server-gateway` deliberately uses flat packages with selective interface abstraction (see gateway ADR-014), because the Gateway's responsibilities are mostly infrastructure orchestration (circuit breaking, load balancing, forwarding) with very little independent business logic of its own.

`service-catalog` is different in kind from the Gateway: it owns real domain logic — a book's lifecycle, its base-status state machine (see ADR-021), validation rules for what constitutes a valid book record, and the decision of when a status change is significant enough to publish as an event. This logic is meaningfully independent of the technology used to persist it (PostgreSQL) or expose it (Spring MVC REST controllers) — the same rationale that justified hexagonal architecture in `service-auth`.

## Decision

**`service-catalog` uses hexagonal (ports-and-adapters) architecture**, following the same layering already established in `service-auth`: Domain (entities, state machine, validation rules) → Application (use cases) → Infrastructure (REST controllers, JPA persistence adapters, Kafka producer adapter).

## Consequences

**Positive:**
- Consistent with `service-auth`'s established pattern — a developer moving between the two services finds the same mental model, reducing onboarding cost within the project itself.
- The book status state machine (ADR-021) and status-change-triggers-event logic (ADR-020) can be unit tested as pure domain logic, without a database or Kafka broker running — matching the fake-adapter testing style already used in `service-auth`.
- Keeps the Kafka producer as a swappable adapter behind a port — if the event schema or even the broker technology changes later, the domain layer (which decides *when* to publish, not *how*) is unaffected.

**Negative / Accepted limitations:**
- More ceremony than a flat package structure for the parts of `service-catalog` that are simple CRUD (e.g., updating a book's title) — accepted as a reasonable tradeoff for consistency with the rest of the codebase and the genuine complexity in the status-change/event-publishing path.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Flat packages with selective abstraction (Gateway's approach) | Gateway's rationale (little independent business logic, mostly infrastructure orchestration) does not apply here — `service-catalog` has real domain rules (state machine, event-worthiness decisions) worth isolating |

## Revisit Triggers

This decision should be revisited if `service-catalog`'s scope turns out to be much simpler than anticipated (pure CRUD with no meaningful domain logic), in which case the hexagonal layering would be unjustified ceremony — unlikely given the state machine and event-publishing responsibilities already scoped.
