# ADR-021: Book Base-Status Model — Unrestricted Transitions

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

`service-catalog` owns a book's **base bookStatus** — `AVAILABLE`, `MAINTENANCE`, `LOST`, `REMOVED` — distinct from borrow/return bookStatus, which belongs entirely to `service-loan`'s domain (see ADR-003). The dedicated bookStatus-change endpoint (ADR-005) needs a defined set of rules for which bookStatus transitions are valid.

Two specific transition questions were raised and answered:
- Can `REMOVED` transition back to `AVAILABLE`? — **Yes** (a book can be soft-deleted in error and restored).
- Can `MAINTENANCE` transition directly to `LOST` (or vice versa) without passing through `AVAILABLE`? — **Yes**.

Both answers remove what would otherwise have been restrictions in a stricter state machine (e.g., requiring `REMOVED` to be a terminal state, or requiring all transitions to pass through `AVAILABLE` as a hub state). With both restrictive options rejected, no meaningful transition restriction remains between any pair of the four statuses.

## Decision

**Any base bookStatus may transition directly to any other base bookStatus.** There is no restricted state machine graph — `AVAILABLE`, `MAINTENANCE`, `LOST`, and `REMOVED` form a fully-connected set of valid transitions. The bookStatus-change endpoint's "validate bookStatus transition" step (BF-05) is reduced to: the requested bookStatus is a valid enum value, and it differs from the current bookStatus (a no-op transition is rejected or treated as idempotent — see open question below).

## Consequences

**Positive:**
- Simplest possible implementation — no transition table, no per-state allowed-next-states logic to build, test, or maintain.
- Matches real-world messiness of library operations: a "lost" book can turn up during a maintenance check (`LOST` → `MAINTENANCE`), a removed record can be restored the moment someone realizes it was deleted by mistake (`REMOVED` → `AVAILABLE`) — modeling every such real scenario as a "valid transition" is easier than trying to anticipate and encode which ones are supposedly impossible.

**Negative / Accepted limitations:**
- No system-level guardrail against a nonsensical rapid sequence of changes (e.g., `AVAILABLE` → `LOST` → `AVAILABLE` → `MAINTENANCE` in quick succession) — any such misuse is a data-entry/process concern, not something `service-catalog` enforces technically. Accepted because the base-bookStatus set is small and each bookStatus is independently meaningful (unlike, say, an order-fulfillment state machine where sequence genuinely matters for correctness).

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Restricted graph (e.g., REMOVED as terminal, or all transitions routed through AVAILABLE) | Explicitly rejected via the two transition questions — both restrictions were found to not match real intended usage |

## Open Question (not blocking, deferred to implementation)

Should setting a bookStatus to its **current** value (e.g., `AVAILABLE` → `AVAILABLE`) be treated as a no-op success (200, no event published) or rejected (409, "no change requested")? This is a minor API-contract detail, not an architectural one — to be resolved when the request/response DTOs are defined.

## Revisit Triggers

This decision should be revisited if additional base statuses are introduced later where sequencing genuinely matters (e.g., a multi-step condition-assessment workflow) — at that point, a subset of transitions may need real restrictions, but that would be a new decision layered on top of this one, not a reversal of it for the current four statuses.
