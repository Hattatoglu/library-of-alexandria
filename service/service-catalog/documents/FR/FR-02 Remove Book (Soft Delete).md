# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-02 Remove Book (Soft Delete)

**Definition:** A book is removed from the catalog via soft delete — the record is retained, marked with a `REMOVED` bookStatus.

- Endpoint: `DELETE /api/v1/books/{bookId}`
- Removal is **unconditional**: service-catalog does not check whether the book is currently on loan before removing it. There is no synchronous call to service-loan (see ADR-003).
- The book's bookStatus is set to `REMOVED`; the underlying database row is **not** physically deleted (see ADR-004).
- A `BookStatusChangedEvent` is published on success (`newStatus=REMOVED`; see FR-05).
- Returns `404 Not Found` if the book does not exist.
- Returns `200 OK` on successful removal.
- A book already in `REMOVED` bookStatus can be removed again — this is a no-op from the caller's perspective (exact response behavior — idempotent 200 vs. 409 — to be finalized at the API contract stage, consistent with the same open question noted in ADR-006 for bookStatus no-ops in general).

**Out of scope:**
- Hard delete — not supported by this endpoint (see ADR-004).
- Blocking removal based on the book's current loan bookStatus — this is not a technical safeguard service-catalog enforces (see ADR-003); any such prevention is a service-loan / UX-level concern.
- Restoring a removed book — handled via FR-04 (bookStatus change back to `AVAILABLE`), not this endpoint.
