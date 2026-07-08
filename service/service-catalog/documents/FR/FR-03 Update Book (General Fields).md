# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-03 Update Book (General Fields)

**Definition:** A book's descriptive metadata (e.g., title, author) can be updated independently of its status.

- Endpoint: `PUT /api/v1/books/{bookId}`
- This endpoint accepts only non-status fields. It never reads or modifies the book's status.
- No Kafka event is published by this endpoint under any circumstances (see ADR-005) — status changes and general updates are deliberately kept as separate, independent operations with independent side-effect contracts.
- Returns `404 Not Found` if the book does not exist.
- Returns `200 OK` with the updated book's representation on success.

**Out of scope:** Any change to the book's status field — attempting to change status through this endpoint is not supported; use FR-04 instead. Whether a request body containing a status field is rejected (`400`) or silently ignored is an API-contract detail to be finalized during implementation.
