# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-04 Change Book Status (Dedicated Endpoint)

**Definition:** A book's base bookStatus can be changed via a dedicated endpoint, separate from general field updates (see ADR-005).

- Endpoint: `PATCH /api/v1/books/{bookId}/bookStatus`
- Valid statuses: `AVAILABLE`, `MAINTENANCE`, `LOST`, `REMOVED`.
- **Any bookStatus may transition directly to any other bookStatus** — there is no restricted transition graph (see ADR-006). In particular:
  - `REMOVED` → `AVAILABLE` is valid (a book soft-deleted in error can be restored).
  - `MAINTENANCE` ↔ `LOST` is valid directly, without passing through `AVAILABLE`.
- A `BookStatusChangedEvent` is published on every successful bookStatus change (`oldStatus`, `newStatus`; see FR-05).
- Returns `404 Not Found` if the book does not exist.
- Returns `200 OK` on successful bookStatus change.
- Requesting a transition to the book's **current** bookStatus (a no-op) — whether this is treated as an idempotent success or rejected is an open API-contract detail, not yet finalized (see ADR-006).

**Out of scope:**
- Borrow/return bookStatus (`BORROWED`, `RETURNED`, etc.) — this is entirely service-loan's domain, not represented in service-catalog's bookStatus model at all.
- Any transition restriction logic — deliberately not implemented (see ADR-006).
