# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-01 Add Book

**Definition:** A new book is added to the catalog with an initial base status.

- Endpoint: `POST /api/v1/books`
- On success, the book is persisted to `CatalogDB` with an initial status of `AVAILABLE`.
- A `BookStatusChangedEvent` is published to the `book-events` Kafka topic on success (`newStatus=AVAILABLE`; see FR-05 for event/topic details).
- Returns `201 Created` with the created book's representation on success.
- Request validation (required fields, format constraints) is enforced before persistence — a book failing validation is not persisted and no event is published.

**Out of scope:** Assigning any status other than `AVAILABLE` at creation time — a book cannot be created directly into `MAINTENANCE`, `LOST`, or `REMOVED`; changing to a different status after creation goes through FR-04.
