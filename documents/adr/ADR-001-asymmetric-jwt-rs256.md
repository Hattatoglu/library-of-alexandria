# ADR-001: Asymmetric JWT (RS256) for Token Signing

| Field        | Value                          |
|--------------|--------------------------------|
| **ID**       | ADR-001                        |
| **Status**   | Accepted                       |
| **Date**     | 2025-06-29                     |
| **Author**   | Eyaz Hattatoğlu                |
| **Deciders** | Eyaz Hattatoğlu                |
| **Tags**     | security, jwt, authentication  |

---

## Context

Library of Alexandria is a microservice-based application. The `service-auth` service is solely
responsible for issuing and managing authentication tokens. Other services (e.g. `api-gateway`,
`service-catalog`, `service-loan`) need to verify access tokens on every incoming request without
introducing a synchronous dependency on `service-auth` for each verification call.

Two fundamental approaches exist for JWT signing:

- **Symmetric (HS256):** A single shared secret is used to both sign and verify tokens.
- **Asymmetric (RS256):** A private key signs tokens; a corresponding public key verifies them.

The architecture requires that the `api-gateway` can verify access tokens **independently**, without
calling `service-auth` on every request. This constraint directly shapes the signing strategy.

---

## Decision

We will use **RS256 (RSA Signature with SHA-256)** for signing JWTs.

- `service-auth` holds the **private key** exclusively and uses it to sign tokens.
- The **public key** is exposed via `GET /api/v1/auth/public-key` as a PEM-encoded string.
- `api-gateway` fetches the public key from `service-auth` **once at startup** and caches it
  in memory. All subsequent token verifications are performed locally using the cached public key.
- Token claims include: `sub` (userId), `username`, `roles`, `type` (`access` or `refresh`),
  `iat`, `exp`.

```
service-auth                api-gateway              downstream service
     |                           |                          |
     |<-- GET /public-key -------|                          |
     |--- public key PEM ------->|                          |
     |                           | (cached in memory)       |
     |                           |                          |
     |              [client request with access_token cookie]
     |                           |                          |
     |                    verify locally                    |
     |                    (no call to service-auth)         |
     |                           |--- forward with headers->|
```

---

## Alternatives Considered

### Option A: Symmetric JWT (HS256)

Every service that needs to verify tokens must share the same secret. In a microservice
architecture this means either:

1. Distributing the secret to all services — increasing the blast radius of a secret leak.
2. Routing all verification through `service-auth` — introducing a synchronous coupling and
   a single point of failure on every authenticated request.

**Rejected** because it violates service autonomy and creates unacceptable latency coupling.

### Option B: Opaque Tokens with Token Introspection (RFC 7662)

Tokens are random strings; verification always requires a call to `service-auth`'s introspection
endpoint.

**Rejected** because it makes `service-auth` a synchronous dependency on every authenticated
request across all services. If `service-auth` is degraded, the entire platform becomes
unauthenticated. The performance and reliability cost outweighs the operational simplicity gain.

### Option C: RS256 with JWKS Endpoint (RFC 7517)

Instead of a single PEM endpoint, expose a JWKS (JSON Web Key Set) endpoint. This is the
industry-standard approach used by Auth0, Keycloak, and AWS Cognito, and enables key rotation
without restarting downstream services.

**Not selected for the current iteration** because it adds implementation complexity
(key ID management, key set rotation) that is not justified at the current scale.
**This option is the planned migration path** when key rotation requirements arise.
See: [Future Considerations](#future-considerations--migration-path).

---

## Consequences

### Positive

- `api-gateway` verifies tokens with zero network calls after startup, eliminating a synchronous
  dependency on `service-auth` for the hot path.
- The private key never leaves `service-auth`. A compromise of any other service does not
  allow token forgery.
- Access tokens are self-contained and stateless; horizontal scaling of any service requires
  no shared session state.

### Negative

- **Key rotation is operationally heavy.** Rotating the private key requires: generating a new
  key pair, updating `service-auth`, restarting `api-gateway` to fetch the new public key, and
  invalidating all existing tokens. This is an accepted trade-off at current scale.
- RSA signature verification is computationally more expensive than HMAC-SHA256. At high token
  verification throughput this may become measurable. Mitigated by caching the public key and
  performing verification locally.
- PEM files must be managed as secrets. They must never be committed to source control.
  Distribution is via environment variables or a secret manager (Vault, AWS Secrets Manager).

---

## Future Considerations / Migration Path

When operational maturity increases or key rotation becomes a requirement:

1. Replace `GET /api/v1/auth/public-key` with `GET /.well-known/jwks.json`.
2. Assign a `kid` (key ID) claim to each issued token.
3. `api-gateway` refreshes the JWKS cache when it encounters an unknown `kid`.
4. This enables zero-downtime key rotation without restarting downstream services.
