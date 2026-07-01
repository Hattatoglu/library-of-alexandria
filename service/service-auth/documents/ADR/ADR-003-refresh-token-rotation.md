# ADR-003: Refresh Token Rotation Strategy

| Field        | Value                              |
|--------------|-------------------------------------|
| **ID**       | ADR-003                            |
| **Status**   | Accepted                           |
| **Date**     | 2025-06-29                         |
| **Author**   | Eyaz Hattatoğlu                    |
| **Deciders** | Eyaz Hattatoğlu                    |
| **Tags**     | security, jwt, refresh-token       |

---

## Context

Access tokens are short-lived (15 minutes, per ADR-001) to limit the blast radius of a leaked
token. Refresh tokens exist to allow the client to obtain a new access token without forcing
re-authentication, and are long-lived (7 days).

A long-lived credential is inherently a higher-value target. If a refresh token is exfiltrated
(e.g. via a compromised network, a malicious browser extension, or physical device access), the
attacker can mint new access tokens for the full 7-day lifetime unless a mitigation is in place.

The core question this ADR resolves: **what happens to a refresh token after it is used?**

---

## Decision

We will implement **refresh token rotation with reuse detection via deletion**, implemented in
`RefreshTokenUseCaseHandler` and `RefreshTokenUseCasePersistenceTokenPortAdapter`.

On every call to `POST /api/v1/auth/refresh`:

1. The presented refresh token is looked up in the `refresh_tokens` table.
2. If found, a **new** access token and a **new** refresh token are generated.
3. The new refresh token is persisted.
4. The **old refresh token is deleted** from the table.
5. If the presented token is not found (`InvalidTokenException`), the request is rejected
   with `401 Unauthorized`.

```
Client                    service-auth                  refresh_tokens table
  |-- POST /refresh -------->|                                  |
  |   (old_token cookie)     |-- findByToken(old_token) ------->|
  |                          |<-- found, userId resolved -------|
  |                          |-- generate new access+refresh -->|
  |                          |-- save(new_token) --------------->|
  |                          |-- deleteByToken(old_token) ------>|
  |<-- new cookies set -------|                                  |
```

Each refresh token is single-use. A token, once exchanged, ceases to exist. There is no
"family" or "chain" tracking of rotated tokens in the current implementation — see
[Future Considerations](#future-considerations).

---

## Alternatives Considered

### Option A: Static Refresh Token (No Rotation)

The refresh token remains valid and reusable for its entire 7-day lifetime, used repeatedly to
mint new access tokens.

**Rejected.** A single token exfiltration grants the attacker a 7-day window of persistent
access, with no signal to the legitimate user or the system that a compromise has occurred.

### Option B: Rotation with Reuse Detection and Token Family Revocation

A more advanced pattern (used by Auth0, Okta) where each refresh token belongs to a "family."
If a token is presented that has *already been rotated out* (i.e. it was used once before),
this is treated as a signal of token theft, and the **entire token family is revoked**,
forcing the legitimate user to re-authenticate.

**Not selected for the current iteration.** This requires additional schema (token family ID,
rotation chain tracking) and additional logic to distinguish "this token was already rotated"
from "this token never existed." Our current schema deletes the old token immediately upon
rotation, which means we cannot currently distinguish a replayed old token from a token that
never existed — both return the same `InvalidTokenException`. **This is the planned upgrade
path**, documented below.

### Option C: Rotation with Deletion Only (Selected)

Each token is single-use; deletion upon use prevents replay. Simpler to implement and reason
about than Option B, at the cost of not being able to detect *and respond to* theft attempts —
we can prevent reuse, but we cannot currently alert on an attempted reuse.

**Accepted** as the appropriate trade-off for the current stage of the project. The security
property that matters most — *a stolen token cannot be used more than once* — is satisfied.
The additional property of *detecting and responding to* a reuse attempt is deferred.

---

## Consequences

### Positive

- A stolen refresh token has a maximum exploitation window of **one use**. Once the legitimate
  client rotates it (which happens automatically before the 15-minute access token expires),
  the stolen copy becomes invalid.
- No server-side session state is needed beyond the single active refresh token per login —
  the system remains largely stateless apart from this single row per session.

### Negative — Concurrent Request Race Condition

If a client fires two simultaneous requests using the same refresh token (e.g. two browser tabs
both detecting an expired access token and refreshing at the same moment), the first request
will succeed and delete the token; the second request will find the token already deleted and
receive a `401 Unauthorized`, forcing that tab into a logged-out state even though the user's
session is legitimately still active in the other tab.

**Accepted risk.** We explicitly chose **not** to implement a grace period (e.g. allowing the
old token to remain valid for a few seconds after rotation) because:

1. It reintroduces a window during which the "old" token is valid twice, weakening the
   single-use guarantee that is the entire point of this ADR.
2. The practical impact is limited to multi-tab scenarios with simultaneous token expiry,
   which is a rare timing coincidence, and is recoverable by the client re-authenticating
   that single tab.

This trade-off should be revisited if user-reported "unexpectedly logged out" issues become
frequent in production telemetry (see `auth.token.refresh.failure{reason=invalid_token}` metric).

### Negative — No Theft Detection Signal

Because the old token is deleted rather than marked as "used," a stolen-and-replayed token
produces the **same error** (`InvalidTokenException`) as a token that simply never existed or
already expired naturally. We cannot currently distinguish "someone is replaying a stolen
token" from "this is just an invalid request," which means we cannot trigger a security alert
or force a full session revocation in response to a detected theft attempt.

---

## Future Considerations

When the platform matures past its current stage, upgrade to **Option B (token family
revocation)**:

1. Add a `family_id` column to `refresh_tokens`, shared across all tokens in a rotation chain.
2. On rotation, do not hard-delete the old token; instead mark it `used_at = NOW()`.
3. If a token with `used_at IS NOT NULL` is ever presented again, treat this as a theft signal:
   revoke the entire `family_id`, forcing full re-authentication, and emit a security alert.
4. This requires a scheduled cleanup job for `used` tokens older than their `expires_at`
   (tracked separately as a production-readiness backlog item).

---

## Related Decisions

- ADR-001: Asymmetric JWT (RS256) — defines the token format being rotated.
- ADR-002: HttpOnly Cookie Token Delivery — defines how the rotated token reaches the client.
- ADR-004: Refresh Token Storage in PostgreSQL — defines where rotation state is persisted.
