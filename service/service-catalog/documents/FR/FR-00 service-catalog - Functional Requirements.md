# service-catalog - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** service-catalog
**Status:** Draft — under discussion
**Last updated:** 2026-07-08

---

## Scope

This document defines **what** service-catalog will do. The reasoning **behind** each decision is tracked separately in ADR documents (`documents/service-catalog/ADR/` and system-wide `documents/ADR/`); this document only defines the behavioral contract.

---

## FR-XX List

1. FR-01 Add Book
2. FR-02 Remove Book (Soft Delete)
3. FR-03 Update Book (General Fields)
4. FR-04 Change Book Status (Dedicated Endpoint)
5. FR-05 Publish Book Status Change Event (Kafka)

## Explicitly Out of Scope for service-catalog

- **Book search / listing** — owned by service-loan, which maintains its own read-model combining catalog data with live loan bookStatus (see ADR-003). service-catalog only exposes detail lookup (`GET /books/{id}`) for drill-down.
- **Borrow / return bookStatus management** — entirely service-loan's domain. service-catalog only owns the book's *base* bookStatus (AVAILABLE, MAINTENANCE, LOST, REMOVED).
- **User notifications** — planned for a future `service-notifier` service, out of scope here.

## Non-Functional Requirements (Next Step)

This document covers functional requirements only. The following should be addressed in a separate **Non-Functional Requirements** document:

- Latency budget for CRUD operations
- Availability target
- Observability / metrics requirements (Kafka publish failure count, bookStatus-change frequency, etc.)
- CatalogDB backup/retention policy for soft-deleted (`REMOVED`) records

## Architecture Decisions (Resolved via ADR)

| # | Topic | Decision |
|---|------|--------|
| 1 | Architecture style | Hexagonal (ports-and-adapters) — see ADR-001 |
| 2 | Web stack | Spring MVC, blocking — see ADR-002 |
| 3 | Dependency on service-loan | None synchronous — event-driven only via Kafka — see ADR-003 |
| 4 | Book removal | Soft delete (bookStatus → REMOVED) — see ADR-004 |
| 5 | Status change API shape | Dedicated endpoint, separate from general update — see ADR-005 |
| 6 | Status transition rules | Unrestricted — any bookStatus to any bookStatus — see ADR-006 |
| 7 | Kafka topic design | Single topic (`book-events`), `bookId` partition key, `eventType` discriminator — see ADR-007 |
| 8 | Producer reliability | Simple post-commit publish (interim); Transactional Outbox planned for later — see ADR-008 |
| — | Message broker (system-wide) | Kafka — see system ADR-001 |

## Documentation Language

Consistent with the rest of the project: all technical documentation, code comments, and application logs are written in **English**.
