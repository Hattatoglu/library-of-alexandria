# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-04 Refresh Token Revocation (DB Delete on Logout)

**Definition:** On logout, ServiceAuth deletes the corresponding refresh token record from its database. No separate Redis blacklist is maintained.

- **ServiceAuth responsibility:** delete the refresh token record from its database on logout.
- Since the refresh token is opaque and DB-backed (not a self-verifying JWT), its validity is inherently determined by the presence of its record in the database. Deleting the record **is** the revocation — no additional deny-list structure is needed.
- If a deleted/unknown refresh token is presented at the refresh endpoint, `401 Unauthorized` is returned.
- The **access token is not revoked on logout**; it remains valid and accepted by the Gateway until its own natural expiry (see accepted risk below).

> **Decision record (superseded designs):** Earlier drafts of this requirement considered a Redis-based blacklist, written either by the Gateway or by ServiceAuth, checked on every request or on refresh only. This was rejected as redundant: a Redis deny-list solves problems that don't apply to this system's current scale (multi-region replication lag, high-throughput DB offloading, cross-instance "logout everywhere" signaling). With a single Postgres instance and DB-backed opaque tokens, DB deletion alone is sufficient and avoids the two-write consistency problem (Redis write + DB delete as separate network calls). This decision should be revisited if the system moves to multi-region deployment or requires immediate cross-instance revocation at high throughput.

> **Accepted risk:** After logout, an already-issued access token remains valid and accepted by the Gateway until it naturally expires (target TTL: ~15 minutes). This is a deliberate tradeoff: immediate revocation of the access token would require a per-request lookup against a revocation store, negating the stateless-JWT benefit. The short access-token TTL bounds the exposure window. This tradeoff should be recorded as an ADR.