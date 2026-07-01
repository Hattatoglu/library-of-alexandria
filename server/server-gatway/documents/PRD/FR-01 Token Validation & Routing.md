# API Gateway - Functional Requirements

**Project:** Library of Alexandria (LibofAlex)
**Component:** API Gateway
**Status:** Draft — under discussion
**Last updated:** 2026-07-01

---

## Scope

This document defines **what** the Gateway component will do. The reasoning **behind** each decision will be tracked separately in ADR documents; this document only defines the behavioral contract.

---

## FR-01 Token Validation & Routing

**Definition:** For every incoming request, the Gateway validates the JWT and routes valid requests to the appropriate downstream service.

- The token is read from an HttpOnly cookie (`access_token`).
- Validation is performed using the RSA public key published by ServiceAuth (JWKS-style endpoint).
- The public key is cached on the Gateway side; it is not fetched on every request.
- If the token is invalid, expired, or has an invalid signature, the request is not forwarded downstream; a `401 Unauthorized` is returned.
- If the token is valid, the resolved user information (`X-User-Id`, `X-User-Role`, etc.) is passed to the downstream service as headers.
- Routing rule: requests are routed based on path prefix (`/catalog/**` → Catalog, `/loans/**` → Loan).

**Out of scope:** Token issuance and refresh flow — these remain ServiceAuth's responsibility.