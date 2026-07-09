# ADR-023: Producer Reliability Guarantee — Simple Post-Commit Publish (Interim)

**Status:** Accepted (interim — see Revisit Triggers)
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

The `CatalogDB` write (bookStatus update) and the Kafka publish (`BookStatusChangedEvent`) are two separate operations, not atomic with each other. Two designs were considered:

1. **Simple post-commit publish:** write to `CatalogDB`, then publish to Kafka as a best-effort follow-up step. If the Kafka publish fails after the DB commit succeeds, the event is lost — `service-loan`'s view silently falls out of sync with `service-catalog`'s actual state until some other event for the same book happens to correct it (or never, if no further changes occur).
2. **Transactional Outbox Pattern:** write the event to an outbox table in the *same* database transaction as the entity update (atomic by construction, since it's a single DB transaction), with a separate poller/CDC process reliably publishing outbox rows to Kafka, retrying until acknowledged. This closes the consistency gap entirely, at the cost of an outbox table, a poller/scheduler process, and its own deep dive to implement correctly.

## Decision

**The simple post-commit publish approach is adopted for now.** `service-catalog` writes to `CatalogDB` first, then publishes to Kafka as a subsequent step, with no distributed-transaction or outbox mechanism bridging the two.

**This is explicitly an interim decision, not a final architectural stance.** The Transactional Outbox Pattern is recognized as the more correct, industry-standard solution to this exact problem — it is the option this project would normally reach for — and is deferred here specifically to keep the current scope manageable, not because it was found unnecessary or inappropriate.

## Consequences

**Positive:**
- Minimal implementation complexity — no outbox table, no poller process, no additional deep dive required before `service-catalog`'s core CRUD + event-publishing functionality can be built and demonstrated.
- Lets the rest of `service-catalog` (domain model, state machine, REST API, Kafka producer integration itself) be built and proven first, with the reliability upgrade as a well-scoped, isolated follow-up improvement rather than a blocking prerequisite.

**Negative / Accepted limitations:**
- **A real consistency gap exists and is knowingly accepted**: if the Kafka publish fails after the DB commit succeeds (broker unreachable, network partition, etc.), the event is silently lost. `service-loan`'s read-model can drift from `service-catalog`'s actual state with no automatic recovery mechanism.
- This gap is not routinely exercised at this system's current scale and expected reliability (a single-broker Kafka setup in a local/portfolio deployment), but it is a genuine correctness weakness, not merely a theoretical one — it should not be forgotten or treated as resolved.

## Alternatives Considered

| Alternative | Reason for deferral (not rejection) |
|---|---|
| Transactional Outbox Pattern | The correct long-term solution; deferred to keep initial `service-catalog` scope focused, not because it was judged unnecessary |

## Revisit Triggers — Planned Migration

The intended path, planned for the future (not tied to a specific hard deadline):

1. Introduce an outbox table (e.g., `book_event_outbox`), written to in the same transaction as the book/bookStatus update.
2. Add a poller (or adopt a CDC tool such as Debezium) that reads unpublished outbox rows and publishes them to Kafka, marking them published only on broker acknowledgment.
3. Retire the direct post-commit publish call in favor of the outbox-driven publish.

This is noted here so the tradeoff isn't forgotten — even once `service-loan` becomes a real consumer of `book-events`, this ADR remains the acknowledged interim state until the migration above is actually carried out.
