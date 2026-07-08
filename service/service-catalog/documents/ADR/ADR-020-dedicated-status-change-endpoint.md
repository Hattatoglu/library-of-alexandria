# ADR-020: Dedicated Status-Change Endpoint

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

A book's base status (`AVAILABLE`, `MAINTENANCE`, `LOST`, `REMOVED`) can change independently of its other fields (title, author, etc.). Two designs were considered for how a status change is triggered via the API:

1. Fold status changes into the general `PUT /api/v1/books/{bookId}` update endpoint, detecting whether the status field differs from the current value and conditionally publishing a Kafka event.
2. A dedicated endpoint (`PATCH /api/v1/books/{bookId}/status`) exclusively for status transitions, separate from general field updates.

## Decision

**Status changes go through a dedicated endpoint**, `PATCH /api/v1/books/{bookId}/status`, separate from the general book-update endpoint (`PUT /api/v1/books/{bookId}`, see BF-04). The general update endpoint never triggers a Kafka event and never touches the status field.

## Consequences

**Positive:**
- Clear separation of concerns at the API level: the general update endpoint's contract (which fields it accepts, that it never has side effects beyond the DB write) stays simple and side-effect-free; the status endpoint's contract (state machine validation, guaranteed Kafka event on success) is explicit and self-contained.
- State machine validation (ADR-006) lives in exactly one place, triggered by exactly one endpoint — no risk of the general update endpoint accidentally bypassing transition rules through a conditional branch that's easy to overlook during future changes.
- Matches REST conventions for representing a genuinely distinct operation (a status transition is a different kind of change than editing descriptive metadata) as its own resource/action, rather than overloading a generic update.

**Negative / Accepted limitations:**
- Two endpoints to call instead of one if a client needs to update both descriptive fields and status in a single user action — accepted as a reasonable tradeoff; such a combined action is uncommon in practice (status changes typically happen independently of metadata edits, e.g. marking a book LOST is a distinct event from correcting its title).

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Status change folded into the general update endpoint, event published conditionally | Mixes two different concerns (metadata editing vs. state machine transition + event publishing) in one endpoint; makes it easier to accidentally introduce a code path that changes status without properly validating the transition or publishing the event |

## Revisit Triggers

This decision should be revisited if client usage patterns show status changes are frequently bundled with metadata edits in a way that makes two separate calls genuinely burdensome — unlikely given the operations represent conceptually distinct actions.
