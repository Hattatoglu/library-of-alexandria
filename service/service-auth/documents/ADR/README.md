# Architecture Decision Records — Library of Alexandria / service-auth

This directory documents the architectural decisions made in the `service-auth` service.
Each ADR captures the context, the alternatives considered, the decision, and the accepted
trade-offs.

## Index

| ID | Title | Status |
|----|-------|--------|
| [ADR-001](ADR-001-asymmetric-jwt-rs256.md) | Asymmetric JWT (RS256) for token signing | Accepted |
| [ADR-002](ADR-002-httponly-cookie-token-delivery.md) | HttpOnly cookie for token delivery | Accepted |
| [ADR-003](ADR-003-refresh-token-rotation.md) | Refresh token rotation strategy | Accepted |
| [ADR-004](ADR-004-refresh-token-postgres-storage.md) | Refresh token storage in PostgreSQL | Accepted |
| [ADR-005](ADR-005-hexagonal-usecase-handler.md) | Hexagonal architecture + UseCase/Handler pattern | Accepted |

## Dependency Map

```
ADR-005 (Hexagonal Architecture)
   │
   │  structural foundation all other decisions are implemented within
   │
   ├── ADR-001 (RS256)
   │      │
   │      └── ADR-002 (HttpOnly Cookie) — delivery mechanism for the token issued via RS256
   │             │
   │             └── ADR-003 (Refresh Token Rotation) — lifecycle of the refresh token carried by the cookie
   │                    │
   │                    └── ADR-004 (PostgreSQL Storage) — where rotation state is persisted
```

## Adding a New ADR

When a new architectural decision is made:

1. Create a new file in the format `ADR-00N-kebab-case-title.md`.
2. Follow the structure used by the existing ADRs: Context → Decision → Alternatives Considered →
   Consequences → (if applicable) Future Considerations → Related Decisions.
3. Update the index table in this README.
4. The `Status` field must be one of: `Proposed`, `Accepted`, `Deprecated`,
   `Superseded by ADR-XXX`.
