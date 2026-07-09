# ADR-022: Kafka Topic Design — Single Topic per Aggregate

**Status:** Accepted
**Date:** 2026-07-08
**Component:** service-catalog

---

## Context

`service-catalog` currently produces a single event type (`BookStatusChangedEvent`), but more event types are anticipated as the system grows (e.g., a future `BookAddedEvent` or `BookMetadataUpdatedEvent`, should bookStatus-change and general-update events ever need to be distinguished more finely than they are today). Two Kafka topic design patterns were considered:

- **One topic per event type** — e.g., `book-bookStatus-changed-events`, `book-added-events`, each carrying a single, narrowly-typed event.
- **One topic per aggregate/entity, with an event-type discriminator field** — a single `book-events` topic carrying all event types about a book, each message tagged with an `eventType` field.

The deciding factor is Kafka's ordering guarantee: messages are only guaranteed to be delivered in order **within a single partition of a single topic**. If events about the same book are split across multiple topics, a consumer (`service-loan`, later `service-notifier`) cannot rely on receiving them in the order they were produced — e.g., a bookStatus-change event could theoretically be processed before the corresponding book's initial creation event, if they live in separate topics with independent consumer lag.

## Decision

**A single topic, `book-events`, carries all event types related to a book.** Each message includes an `eventType` field to distinguish event types (currently only `STATUS_CHANGED`, per BF-02/BF-03/BF-05 — all three flows publish the same `BookStatusChangedEvent` shape). The topic is partitioned using **`bookId` as the partition key**, so all events for a given book are guaranteed to land in the same partition and be delivered to consumers in production order.

## Consequences

**Positive:**
- Preserves per-book event ordering across all current and future event types — a consumer processing events for a given book always sees them in the order `service-catalog` produced them.
- Avoids topic proliferation as new event types are added — no new topic (and associated consumer group management, retention configuration, monitoring) needed just because a new kind of book-related event is introduced.
- Consumers (like the future `service-loan`) subscribe to one topic to get the complete picture for a book, rather than needing to subscribe to and merge multiple topics.

**Negative / Accepted limitations:**
- Consumers must filter/branch on the `eventType` field themselves rather than relying on topic subscription to pre-filter event types — a minor complexity shifted from topic configuration to consumer code, considered a reasonable tradeoff for the ordering guarantee gained.
- All consumers of `book-events` receive all event types, even ones they don't care about (e.g., a hypothetical consumer only interested in bookStatus changes still receives every event type on the topic) — acceptable at the current scale (one event type today, a small number anticipated).

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| One topic per event type | Loses cross-event-type ordering guarantees for the same book, since Kafka only orders within a single topic's partition — a real risk once more than one event type exists for the same entity |

## Revisit Triggers

This decision should be revisited if the number of event types per book grows large enough, or diverges enough in audience (very different sets of consumers caring about very different event types), that the "one consumer receives everything" cost outweighs the ordering guarantee's value — not expected at this system's current scale.
