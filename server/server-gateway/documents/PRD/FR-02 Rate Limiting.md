# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-02 Rate Limiting

**Definition:** The Gateway limits clients that send an excessive number of requests.

- Limits are applied **per user** (JWT subject); for unauthenticated requests, IP-based limiting is used as a fallback.
- Rate limit state is stored in Redis so it can be shared across multiple Gateway instances (not kept in memory).
- When the limit is exceeded, `429 Too Many Requests` is returned; a `Retry-After` header is included in the response.
- **Algorithm: Token bucket.** Chosen for its simplicity in a Redis-backed implementation (`INCR` + `EXPIRE`) and because it tolerates short bursts while still enforcing a long-term average rate — a good fit for the project's scale. Bucket capacity and refill rate are configuration parameters, to be finalized in an ADR.