# ADR-005: Hexagonal Architecture with UseCase/Handler Pattern

| Field        | Value                                   |
|--------------|-------------------------------------------|
| **ID**       | ADR-005                                  |
| **Status**   | Accepted                                 |
| **Date**     | 2025-06-29                               |
| **Author**   | Eyaz Hattatoğlu                          |
| **Deciders** | Eyaz Hattatoğlu                          |
| **Tags**     | architecture, hexagonal, domain-design   |

---

## Context

`service-auth` is the first of five planned microservices in Library of Alexandria
(`api-gateway`, `service-auth`, `service-catalog`, `service-loan`, plus `nginx` as edge proxy).
Architectural patterns established here will be replicated across the remaining services, so
the cost of this decision is multiplied by however many services follow the same template.

The core tension being resolved: **how tightly should business logic be coupled to the
frameworks and infrastructure (Spring, JPA, PostgreSQL, JWT libraries) that implement it?**

A conventional layered architecture (Controller → Service → Repository) tends to let
infrastructure concerns leak into business logic over time — a `@Service` class commonly ends
up directly depending on JPA entities, Spring Security types, or HTTP-specific exceptions,
making the core logic difficult to unit test without a Spring context and difficult to
reason about independently of its delivery mechanism.

---

## Decision

We will structure `service-auth` using **Hexagonal Architecture (Ports and Adapters)**,
combined with a **UseCase/Handler pattern** for application logic, per package:

```
domain/usecase/{usecase-name}/
  ├── handler/      → the UseCase data object + its Handler (pure business logic)
  └── port/         → interfaces the handler depends on (no implementation)

infra/
  ├── postgres/adapter/   → implements *PersistenceAuthPort, *PersistenceTokenPort
  ├── security/adapter/   → implements *SecurityPort
  └── rest/api/           → Controllers — translate HTTP ↔ UseCase objects
```

**Core rules enforced across the codebase:**

1. Classes under `domain/` **never** import `infra/` types, Spring annotations beyond `@Service`,
   JPA, or any framework-specific exception.
2. Every dependency a `Handler` needs is expressed as a **Port** (a plain Java interface) defined
   *within* the `domain` package of that use case.
3. Each `Handler` implements `UseCaseHandler<T extends UseCase>`, exposing a single
   `handle(T usecase)` method — one method, one responsibility, no overloaded "service" classes
   with a dozen unrelated methods.
4. Infrastructure adapters (`infra/`) implement the ports defined by the domain. The domain
   defines the contract; infrastructure conforms to it — not the other way around.
5. The `UseCase` object itself (e.g. `SignUpUser`, `LoginUser`, `RefreshTokenUseCase`) is a
   plain Java object carrying both input and accumulated output state through the handler's
   internal pipeline. It is not a JPA entity, not a DTO, and is never directly serialized to
   HTTP — controllers map between REST DTOs and UseCase objects explicitly.

```
HTTP Request
     │
     ▼
RegisterController  ──(maps DTO → UseCase)──▶  SignUpUser
     │                                              │
     │                                              ▼
     │                                     SignUpUserHandler.handle()
     │                                              │
     │                              ┌───────────────┼───────────────┐
     │                              ▼                                ▼
     │              SignUpUserPersistenceAuthPort         (pure domain logic:
     │              (interface, defined in domain)         role assignment,
     │                              │                       UUID generation)
     │                              ▼
     │              SignUpUserPersistenceAuthPortAdapter
     │              (infra/postgres — implements the port,
     │               talks to UserAuthRepository / JPA)
     ▼
HTTP Response  ◀──(maps UseCase → DTO)──  SignUpUser (populated)
```

---

## Alternatives Considered

### Option A: Conventional Layered Architecture (Controller → Service → Repository)

The default Spring Boot tutorial pattern. `@Service` classes directly depend on
`@Repository` interfaces (typically Spring Data JPA repositories) and JPA entities.

**Rejected.** While faster to write initially, this pattern tends to erode over time:
JPA entities get used directly in business logic (then leak into API responses), and
business logic becomes untestable without spinning up a Spring/JPA context, because the
"service" layer is directly coupled to persistence types rather than to an abstraction.

### Option B: Hexagonal Architecture, but with a Traditional "Service" Layer (no Handler pattern)

Use ports and adapters for infrastructure boundaries, but keep one large `@Service` class per
aggregate (e.g. a single `AuthService` with `register()`, `login()`, `logout()`,
`refreshToken()`, `updateRole()` methods), rather than one Handler per use case.

**Rejected.** A single multi-method service class accumulates dependencies for *all* of its
methods combined, even though any given test or any given request only exercises one method.
This makes constructor injection lists grow unboundedly and makes it harder to reason about
exactly which dependencies a given operation actually needs. The UseCase/Handler pattern keeps
each class's dependency list scoped to exactly what one operation requires.

### Option C: Hexagonal Architecture with UseCase/Handler Pattern (Selected)

**Accepted.** Combines strict domain isolation (ports and adapters) with a one-class-per-operation
structure (UseCase/Handler), maximizing both testability and the principle of single
responsibility at the class level.

---

## Consequences

### Positive

- **Testability without a framework context.** Every `Handler` can be unit tested using
  hand-written fake implementations of its ports — no Spring context, no Mockito, no database.
  This is demonstrated directly in the test suite (`CreateUserHandlerTest`,
  `LoginUserHandlerTest`, etc.), where fake ports are plain inner classes implementing the
  domain-defined interfaces.
- **Infrastructure is replaceable without touching business logic.** Switching the persistence
  technology from PostgreSQL to a different store, or swapping the JWT library, requires writing
  a new adapter that implements the same port — the `Handler` and `UseCase` classes are
  untouched.
- **Each Handler's dependencies are explicit and minimal.** Reading a Handler's constructor
  tells you exactly what that one operation needs — nothing more, nothing inherited from
  unrelated operations bundled into the same class.
- **Consistent template across services.** Because this pattern is established in
  `service-auth` first, `service-catalog` and `service-loan` can replicate the same package
  structure and the same testing approach, reducing the architectural decision-making cost for
  every subsequent service.

### Negative

- **File and package count is significantly higher** than a conventional layered approach. A
  single "register a user" operation spans a `UseCase` class, a `Handler` class, one or more
  `Port` interfaces, and one or more `Adapter` implementations — five-plus files for what a
  layered architecture might express in one `@Service` method. This is an explicit, accepted
  trade-off of structure for testability and decoupling.
- **Steeper onboarding curve.** A developer unfamiliar with hexagonal architecture or this
  specific package convention will need ramp-up time to understand *where* a given piece of
  logic lives — this ADR, together with the project's class diagrams
  (`documents/class-diagrams/`), is the primary onboarding artifact intended to shorten that
  curve.
- **Naming discipline is required and has not always been consistently applied.** Adapter class
  names in the current codebase (e.g. early iterations like
  `LoginUserRefreshTokenPersistenceTokenTokenPortAdapter`) have suffered from mechanical
  concatenation of Port interface names. A naming convention —
  `{UseCaseName}{Concern}{Store|Auth}PortAdapter` — should be applied consistently; deviations
  are a code-quality issue to be corrected, not a flaw in the architecture itself.

---

## Related Decisions

- All other ADRs in this set describe decisions made *within* the boundaries this architecture
  establishes — e.g. ADR-001 (JWT signing) is implemented behind a `*SecurityPort`, and ADR-004
  (token storage) is implemented behind a `*PersistenceTokenPort`. This ADR is the structural
  foundation the others build on.
