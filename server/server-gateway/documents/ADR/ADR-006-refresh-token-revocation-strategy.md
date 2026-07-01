# ADR-001: Refresh Token Revocation Strategy

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway, ServiceAuth

---

## Context

On logout, the system needs to revoke the user's refresh token so it can no longer be used to obtain new access tokens. Two designs were considered during discussion:

1. A Redis-based blacklist, populated on logout and checked before honoring a refresh request.
2. Direct deletion of the refresh token record from ServiceAuth's database.

The refresh token in this system is an **opaque token** (a `SecureRandom`-generated, Base64URL-encoded value, not a self-verifying JWT), stored as a row in ServiceAuth's PostgreSQL database. Its validity is already determined by looking up that row at refresh time — the token carries no embedded claims that can be verified without a database round trip.

Two ownership models for a Redis blacklist were also discussed:
- Gateway writes to Redis on logout, ServiceAuth deletes from its DB (two independent writes, two services).
- ServiceAuth writes to Redis and deletes from DB (single service, but still two stores).

Both introduce a two-write consistency problem: if one write succeeds and the other fails, the token ends up in an inconsistent state (revoked in one store but not the other).

A Redis blacklist is a well-established pattern for revoking **stateless** tokens (e.g., JWTs), where the token itself cannot express "this has been revoked" — an external deny-list is the only way to invalidate it before its natural expiry. It also has value in specific production scenarios: multi-region deployments where DB deletion needs to propagate before read replicas catch up, high-throughput systems offloading revocation checks from the primary database, and "logout everywhere" flows needing a fast, centralized revocation signal.

## Decision

**No Redis blacklist is maintained for refresh tokens.** On logout, ServiceAuth deletes the refresh token record from its database. Because the refresh token is opaque and DB-backed, deleting the record **is** the revocation — a token with no matching database row is inherently invalid at the next refresh attempt.

## Consequences

**Positive:**
- Eliminates the two-write consistency problem entirely — there is a single source of truth (the database row) and a single write operation.
- Removes an entire dependency (Redis) from the logout/revocation path, simplifying the Gateway (it no longer needs write access to a blacklist) and ServiceAuth's logout flow.
- Matches the system's current scale: single Postgres instance, single-region deployment, no requirement for sub-second cross-instance revocation propagation.

**Negative / Accepted limitations:**
- If the system later moves to a multi-region deployment with read replicas, DB deletion may not be instantly visible to all replicas, reintroducing a case where a Redis-based fast-propagation signal would help.
- If refresh-token validation is ever moved off the critical DB read path for performance reasons (e.g., to reduce load on Postgres at high throughput), a negative cache in Redis may become useful again — as a performance optimization, not a correctness requirement.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Redis blacklist written by Gateway, DB deleted by ServiceAuth | Two independent writes across two services; no transactional guarantee between them |
| Redis blacklist written and read by ServiceAuth only | Still a redundant second store on top of an already-authoritative DB row; adds complexity without solving a problem this system currently has |

## Revisit Triggers

This decision should be revisited if:
- The system moves to multi-region deployment with database read replicas.
- Refresh-token validation needs to be decoupled from a direct database read for throughput reasons.
- A "logout everywhere across all sessions instantly" requirement is introduced.
