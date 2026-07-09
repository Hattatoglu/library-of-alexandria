# ADR-019: Soft Delete for Book Removal

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

When a book is removed from the catalog (`DELETE /api/v1/books/{bookId}`), `service-catalog` needs to decide whether this is a hard delete (row physically removed from the database) or a soft delete (row retained, marked with a `REMOVED` bookStatus).

Given ADR-003 (no synchronous dependency on `service-loan`), `service-catalog` has no way to know at delete time whether a book has loan history or an active loan referencing it. A hard delete risks breaking referential integrity for any historical record — in `service-loan`, or in `service-catalog`'s own audit trail — that references the deleted book by ID.

## Decision

**Book removal is a soft delete**: the book's row is retained in `CatalogDB`, with its bookStatus set to `REMOVED` (see ADR-006 for the bookStatus model, which explicitly allows transitioning back out of `REMOVED`).

## Consequences

**Positive:**
- No referential integrity risk — any external reference to a book ID (e.g., in `service-loan`'s loan history, once that service exists) remains valid indefinitely.
- Directly enables ADR-006's decision that `REMOVED` is not a dead-end state — a book removed in error can be transitioned back to `AVAILABLE` without needing to reconstruct a deleted record.
- Consistent with the accepted risk in ADR-003 (a borrowed book can be soft-deleted) — because it's soft-delete, this is a bookStatus change, not data loss, keeping the consequence of that accepted risk low.

**Negative / Accepted limitations:**
- `CatalogDB` accumulates `REMOVED` rows indefinitely (no automatic purging in scope here) — acceptable at this system's scale; a retention/archival policy can be introduced later without changing this ADR's core decision.
- All catalog queries must be aware of the `REMOVED` bookStatus and filter it out where appropriate (e.g., a `GET /books/{id}` detail lookup for a removed book should presumably still return it — TBD at the API contract stage — but any future listing/search built directly against `service-catalog`, if one is ever added, must not silently include removed books).

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Hard delete | Risks breaking referential integrity for any historical reference to the book (loan history in service-loan, audit trails); also incompatible with ADR-006's decision to allow REMOVED → AVAILABLE recovery |

## Revisit Triggers

This decision should be revisited if the volume of removed books becomes operationally significant enough to require an explicit archival/purge strategy — not an architectural reversal, just an addition (e.g., a scheduled job moving very old REMOVED records to cold storage).
