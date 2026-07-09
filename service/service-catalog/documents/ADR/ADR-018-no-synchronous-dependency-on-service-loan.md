# ADR-018: No Synchronous Dependency on service-loan

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

Two operations initially raised the question of whether `service-catalog` needs to synchronously check a book's loan bookStatus:

1. **Book search/listing** — should a user browsing the catalog see live borrow-bookStatus information?
2. **Book removal (soft-delete)** — should `service-catalog` block removal of a book that is currently on loan?

For (1), it was decided that search/listing is not `service-catalog`'s responsibility at all — `service-loan` owns the user-facing search/listing experience, presumably maintaining its own read-model (populated via Kafka events from `service-catalog`, see ADR-015 system-wide) that combines catalog data with live loan bookStatus. `service-catalog` only exposes a detail-lookup endpoint (`GET /books/{id}`) for drill-down, not a search/listing endpoint.

For (2), the question was whether `service-catalog` should call `service-loan` synchronously before allowing a delete, to prevent removing a book that's currently borrowed. This was explicitly decided against.

## Decision

**`service-catalog` has no synchronous (request/response) dependency on `service-loan`, in either direction.** Book removal is unconditional (see ADR-019) — `service-catalog` does not check loan bookStatus before soft-deleting a book. The only relationship between the two services is asynchronous: `service-catalog` publishes `BookStatusChangedEvent`s to Kafka; `service-loan` consumes them independently (system-wide ADR-001).

Preventing removal of a currently-borrowed book, if desired, is a workflow/UX concern — surfaced to the user through `service-loan`'s search/listing view (where loan bookStatus is visible) — not a technical cross-service check enforced by `service-catalog` at delete time.

## Consequences

**Positive:**
- `service-catalog`'s availability is fully decoupled from `service-loan`'s — a `service-loan` outage cannot block or slow down any `service-catalog` operation (add, remove, update, bookStatus change).
- Avoids the classic distributed-systems trap of a "simple" synchronous check silently becoming a hard dependency: what starts as "just check loan bookStatus before delete" tends to grow into more synchronous coupling over time.
- Consistent with the Gateway's own hard-won lessons about avoiding unnecessary synchronous coupling and check-then-act patterns (see gateway ADR-013's fail-open reasoning, and the repeated check-then-act race-condition avoidance throughout the Gateway's design).

**Negative / Accepted limitations:**
- A book can be soft-deleted while still on loan, with no technical safeguard preventing it. This is an accepted risk — the actual harm is limited (soft-delete, not hard-delete, so the record and its history remain recoverable/auditable; see ADR-019), and the intended mitigation (visibility via `service-loan`'s UI) is a product-level, not data-integrity-level, safeguard.
- If `service-loan`'s Kafka consumer falls behind or is down, its view of book bookStatus (including removal) can be stale for an unbounded period until it catches up — an accepted consequence of the eventual-consistency model already accepted in system-wide ADR-015.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Synchronous REST call to service-loan before delete, blocking removal if borrowed | Couples service-catalog's availability and latency to service-loan's; contradicts the event-driven, decoupled design already established for bookStatus changes |
| service-catalog maintaining its own cached view of loan bookStatus (via consuming service-loan's events) just to gate deletion | Adds meaningful complexity (service-catalog becoming a Kafka consumer, not just a producer) for a safeguard whose absence has limited, recoverable impact given soft-delete |

## Revisit Triggers

This decision should be revisited if removing a borrowed book is found to cause real operational or data-integrity problems in practice (e.g., if `service-loan` cannot gracefully handle a "removed" bookStatus for a book it still has an active loan record for) — at that point, service-catalog consuming service-loan's own events (rather than a synchronous call) would be the preferred fix, keeping the no-synchronous-dependency principle intact.
