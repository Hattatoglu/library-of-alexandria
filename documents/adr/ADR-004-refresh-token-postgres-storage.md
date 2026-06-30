# ADR-004: Refresh Token Storage in PostgreSQL

| Field        | Value                              |
|--------------|-------------------------------------|
| **ID**       | ADR-004                            |
| **Status**   | Accepted                           |
| **Date**     | 2025-06-29                         |
| **Author**   | Eyaz Hattatoğlu                    |
| **Deciders** | Eyaz Hattatoğlu                    |
| **Tags**     | persistence, database, redis       |

---

## Context

Refresh tokens (ADR-003) require server-side persistence: the system must be able to look up a
token by its value, resolve its owning user, delete it on rotation, and — eventually — revoke
all tokens for a user (e.g. on password change or "log out everywhere").

Two storage candidates are commonly used for this kind of short-to-medium-lived, high-write,
key-lookup data:

1. **PostgreSQL** — the relational database already used by `service-auth` for user accounts.
2. **Redis** — an in-memory key-value store, commonly used elsewhere in the platform's design
   (the `api-gateway`'s access-token **blacklist**, per the system's sequence diagrams, already
   uses Redis).

This ADR addresses *only* refresh token storage. The access-token blacklist in `api-gateway`
is a separate, already-decided use of Redis and is not reconsidered here.

---

## Decision

We will store refresh tokens in **PostgreSQL**, in the `refresh_tokens` table:

```sql
CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
```

This keeps refresh token storage **co-located with the user data it relates to**, within the
same transactional boundary, using the same database technology already operated for
`service-auth`.

---

## Alternatives Considered

### Option A: Redis with TTL (Selected by neither, evaluated here)

Store the refresh token as a Redis key (`refresh_token:{token} -> userId`) with a native TTL
matching the 7-day expiration, letting Redis handle expiry automatically.

**Rejected for this use case**, for the following reasons:

- **No `expires_at` query capability.** Redis TTL is opaque — there is no way to run an
  analytical or administrative query like "show all tokens expiring in the next 24 hours" or
  "show all active sessions for this user," both of which are realistic operational and support
  requirements (e.g. responding to a user's "log me out everywhere" request, or a security
  investigation).
- **No native foreign key / cascade relationship to the `users` table.** If a user account is
  deleted, the relational `ON DELETE CASCADE` on `refresh_tokens.user_id` guarantees orphaned
  tokens cannot exist. Achieving the equivalent in Redis requires an explicit application-level
  cleanup step, which is an extra failure mode (the cleanup step is forgotten, or fails, or
  races with token issuance).
- **No audit trail.** PostgreSQL's `created_at` column and the durability of a relational table
  provide a natural foundation for audit logging of session creation, which Redis's
  fire-and-forget TTL model does not provide without additional instrumentation.
- **Introduces a second stateful dependency to operate.** `service-auth` already operates and
  is operationally responsible for PostgreSQL. Adding Redis as a second stateful dependency
  for this specific concern introduces additional infrastructure (failover, backup, monitoring)
  for a problem PostgreSQL already solves adequately at current scale.

### Option B: PostgreSQL (Selected)

**Accepted.** Refresh tokens are relational data with a clear foreign-key relationship to
`users`, a need for ad-hoc querying (active sessions per user, expiring-soon tokens), and a
requirement for cascading deletes. PostgreSQL satisfies all of these natively, using
infrastructure `service-auth` already operates.

---

## Consequences

### Positive

- `ON DELETE CASCADE` on the `user_id` foreign key guarantees referential integrity — a deleted
  user can never leave orphaned refresh tokens, without any application-level cleanup code.
- Token lookups (`findByToken`) and rotations are part of the same transactional boundary as
  other auth operations, simplifying reasoning about consistency.
- No additional infrastructure to provision, secure, monitor, or back up beyond what
  `service-auth` already requires for user data.
- Enables future operational queries (active session count, expiring tokens, per-user session
  list) without additional tooling.

### Negative

- **No automatic expiry.** Unlike Redis's native TTL, PostgreSQL does not natively expire rows.
  Expired refresh tokens (`expires_at < NOW()`) will accumulate in the table indefinitely unless
  actively cleaned up.

  **This is an accepted, explicitly tracked gap.** A scheduled cleanup job
  (`DELETE FROM refresh_tokens WHERE expires_at < NOW()`) is required before production
  deployment and is tracked as a separate backlog item (see the project's architect checklist,
  Phase 4: Production Readiness).

- **Higher read/write latency than an in-memory store** for the specific operation of "look up
  token by value." At current expected scale (a single-tenant library lending system, not a
  high-throughput consumer platform) this is not a measurable concern. Should refresh-token
  lookup ever become a measured bottleneck (visible via the `auth.token.refresh.duration`
  metric), introducing a Redis-backed read cache in front of this table — without changing the
  source of truth — is the natural next step, rather than a full migration to Redis as the
  primary store.

---

## Related Decisions

- ADR-001: Asymmetric JWT (RS256)
- ADR-003: Refresh Token Rotation Strategy — defines the read/write/delete pattern this storage
  must support.
