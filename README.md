# Library of Alexandria (LibofAlex)

A microservices-based book lending / reservation system, built as a portfolio project and a
practice ground for senior-level system design interviews.

> **Status:** Active development. Not intended for production use. Several components
> (API Gateway, Catalog, Loan) are still work-in-progress; `service-auth` is the most
> mature module.

---

## ⚠️ Security Notice — Read Before Cloning

This repository intentionally contains a **demo RSA key pair**
(`service/service-auth/src/main/resources/keys/`), committed to source control so the
project runs out-of-the-box without any manual setup.

- **These keys are for local development and demonstration only.**
- They are **not used anywhere outside this repository** and grant no access to any real system.
- **Do not reuse them.** If you fork this project for anything beyond local experimentation,
  generate your own key pair and load it via environment variables or a secrets manager
  (see [ADR-001](./service/service-auth/documents/adr/ADR-001-asymmetric-jwt-rs256.md)).
- In a real production deployment, private keys must **never** be committed to source
  control — this repository deliberately trades that best practice for reproducibility,
  since the project's goal is portfolio/interview demonstration, not production deployment.

---

## What This Project Is

Library of Alexandria simulates a library's book lending platform, decomposed into
independently deployable microservices behind a single API Gateway. It's used to practice:

- Hexagonal / Clean Architecture in a real, non-trivial domain
- The UseCase + Handler pattern with strict OCP compliance
- Token-based authentication (RS256 JWT + opaque refresh tokens) across service boundaries
- Architecture Decision Records (ADRs) as a discipline, not an afterthought
- System design interview practice (estimation, API design, HLD, deep dives, bottlenecks)

---

## Architecture Overview

```
                        ┌──────────┐
        Internet ─────► │  Nginx   │   (only public-facing component)
                        └────┬─────┘
                             │  internal docker network only
                             ▼
                       ┌─────────────┐
                       │ API Gateway │  (WebFlux, reactive)
                       │  - JWT verify (cached public key)
                       │  - rate limiting (Redis)
                       │  - circuit breaker
                       │  - routing
                       └──────┬──────┘
             ┌────────────────┼────────────────┐
             ▼                ▼                 ▼
      ┌─────────────┐  ┌──────────────┐  ┌─────────────┐
      │ service-auth│  │service-catalog│  │service-loan │
      └─────────────┘  └──────────────┘  └─────────────┘
             │
             ▼
       ┌───────────┐
       │ PostgreSQL│
       └───────────┘
```

**Trust boundary:** the Gateway is the only component that validates JWTs directly.
Once validated, it forwards resolved identity (`X-User-Id`, `X-User-Role`) to downstream
services as headers. This means downstream services currently rely on network isolation
(no service port other than Nginx is exposed externally) as their trust boundary — see
[Known Limitations](#known-limitations) below.

---

## Modules

| Module | Description | Status |
|---|---|---|
| `artifactory/lib-domain` | Shared domain primitives (`UseCase`, `UseCaseHandler`) | Stable |
| `artifactory/lib-infra-web` | Shared web exception handling support | Stable |
| `artifactory/lib-db-postgres` | Shared Postgres infrastructure helpers | Stable |
| `service/service-auth` | Authentication & authorization service (JWT issuance, refresh rotation, roles) | Mature |
| `server/server-gatway` | Reactive API Gateway (JWT validation, rate limiting, routing) | In progress |
| `service-catalog` *(planned)* | Book catalog management | Not started |
| `service-loan` *(planned)* | Loan / reservation lifecycle | Not started |

Each module maintains its own `documents/adr` folder with Architecture Decision Records
explaining the reasoning behind non-obvious choices.

---

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5 (Spring WebFlux for the Gateway, Spring MVC for services)
- **Auth:** JJWT (RS256), Spring Security
- **Persistence:** PostgreSQL, Flyway migrations
- **Caching / Rate limiting:** Redis
- **Testing:** JUnit 5, Testcontainers, Cucumber (BDD acceptance tests)
- **Observability:** Micrometer, Prometheus, Grafana
- **Build:** Maven (multi-module)
- **Containerization:** Docker, Docker Compose

---

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+
- Docker & Docker Compose

### Build

```bash
make maven
```

### Run service-auth locally

```bash
make server-postgres      # starts Postgres
make build-service-auth   # builds jar + docker image
make service-auth         # starts the service
```

Service will be available at `http://localhost:8080`.

### Run database admin UI (optional)

```bash
make server-adminer
```

---

## Documentation

- [`documents/usecase`](./documents/usecase) — use case specifications per domain flow
- Per-module `documents/adr` — architecture decision records
- Per-module `documents/PRD` (where present) — functional requirements

---

## Known Limitations

This project is a work in progress and intentionally documents its own gaps rather than
hiding them:

- **No internal zero-trust layer yet.** Downstream services currently trust the
  `X-User-Id` / `X-User-Role` headers forwarded by the Gateway without an additional
  service-identity check (mTLS or shared internal secret). Security currently depends on
  network isolation holding. Planned as a follow-up hardening step.
- **Role-based authorization is partially wired.** Some administrative endpoints do not
  yet enforce role checks at the application layer — tracked as a priority fix.
- **Gateway is still under active development** and not yet feature-complete.

---

## License

Personal portfolio project. No license granted for reuse of the demo credentials or
production deployment as-is.
