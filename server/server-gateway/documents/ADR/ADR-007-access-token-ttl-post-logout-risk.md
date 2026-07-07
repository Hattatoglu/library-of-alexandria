# ADR-002: Access Token TTL and Post-Logout Exposure Risk

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway, ServiceAuth

---

## Context

Following ADR-001, logout revokes the refresh token but does not touch the access token. The access token is a short-lived, self-verifying JWT (RS256), validated by the Gateway via signature check against ServiceAuth's public key — with no database or cache lookup involved (see FR-01 Token Validation & Routing).

This raises the question: after a user logs out, should their still-valid access token continue to be accepted by the Gateway until it naturally expires, or should it be immediately revoked?

Immediate revocation of a stateless JWT requires an external revocation store (e.g., a Redis deny-list keyed by the token's `jti` claim) that the Gateway checks on **every** request. This defeats the primary architectural benefit of using a stateless JWT for the access token — namely, that the Gateway can validate it with a pure signature check and no per-request I/O.

## Decision

**The access token is not revoked on logout.** It remains valid and is accepted by the Gateway until it reaches its own expiry. The access token TTL is kept short (target: ~15 minutes) specifically to bound the exposure window this creates.

## Consequences

**Positive:**
- The Gateway's access-token validation path (FR-01) stays a pure, stateless signature check — no Redis or database dependency on the hot request path, which is consistent with the reactive, non-blocking design of the Gateway.
- Simpler failure mode: there is no revocation store to keep consistent, no additional write on logout beyond the refresh-token deletion in ADR-001.

**Negative / Accepted risk:**
- After logout, an attacker or the previous session holder in possession of a still-valid access token can continue to make authenticated requests for up to the remaining TTL (bounded by ~15 minutes).
- This is accepted as a deliberate tradeoff, not an oversight. The short TTL is the control that bounds the risk.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Blacklist access token `jti` in Redis on logout, checked on every request | Requires a Redis lookup on every downstream-routed request, negating the stateless-JWT performance benefit; adds a second revocation mechanism alongside ADR-001's refresh-token deletion |
| Very short access token TTL (e.g., 1–2 minutes) with aggressive refresh | Reduces exposure window further but increases refresh-endpoint traffic and client complexity; not adopted as the default, but the TTL value itself remains a tunable parameter |

## Revisit Triggers

This decision should be revisited if:
- A security requirement emerges for immediate, hard revocation on logout (e.g., compliance requirement, incident-response need).
- The access token TTL needs to be lengthened for product reasons (e.g., to reduce refresh frequency), which would widen the accepted exposure window and may no longer be an acceptable tradeoff.
