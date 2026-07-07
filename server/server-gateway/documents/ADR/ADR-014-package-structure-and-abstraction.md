# ADR-014: Package Structure — Flat Packages with Selective Interface Abstraction

**Status:** Accepted
**Date:** 2026-07-01
**Component:** API Gateway

---

## Context

`service-auth` uses hexagonal/ports-and-adapters architecture (Application → Domain → Persistence), which fits it well: it has substantial domain logic (user registration rules, password policy, token lifecycle rules) worth isolating from infrastructure concerns like Postgres, JJWT, or Spring Security.

The Gateway's responsibilities are different in kind. Its core behaviors — circuit breaking, load balancing, request forwarding, public key fetching — are almost entirely orchestration of infrastructure libraries (Resilience4j, WebClient, Redis) with very little independent business logic of the Gateway's own. Wrapping these in ports and adapters would mean building an abstraction layer whose only implementation is the infrastructure it's abstracting — the port would never have a second, meaningfully different adapter, and the domain layer behind it would contain almost no logic of its own. That is ceremony without payoff: it doesn't buy testability (Resilience4j's own state machine is not something the Gateway tests — its own wiring is), and it doesn't isolate any decision that's actually the Gateway's to make.

Two areas were identified as different: they contain logic that is conceptually independent of the technology used to implement it, and where isolating that logic behind an interface has a concrete payoff (testing the decision itself without a live Redis instance or a live routing table, mirroring the fake-adapter testing style already used in `service-auth`):

- **Rate limit decision:** "is this request within the allowed rate?" is a pure decision (`currentCount <= capacity`) independent of whether the counter is read from Redis, an in-memory map, or anywhere else.
- **Route authorization decision:** "is this role allowed to access this path?" is a pure decision independent of how the role was extracted or how the routing table is stored.

## Decision

**The Gateway uses flat, single-level packages** (`security`, `ratelimit`, `routing`, `config`, `exception`, `logging` — no `domain`/`application`/`infra` layering, no ports-and-adapters directory structure).

Within this flat structure, **two specific decisions are abstracted behind a plain interface**, each with a production implementation and — where useful — a fake implementation for tests:

- `RateLimitDecision` (interface) — decides allow/deny given a count and a capacity, independent of the storage backend. Production implementation reads from Redis via the token-bucket Lua script (ADR-003); tests can use a fake in-memory counter.
- `RouteAuthorizationDecision` (interface) — decides whether a given role may access a given route, independent of where the routing/role table is configured. Production implementation reads from Gateway configuration; tests can use a fake fixed table.

Everything else (JWT signature validation and public key caching, circuit breaker wiring, load balancer instance selection, request forwarding, actuator/exception/logging configuration) is implemented as plain classes with no interface indirection — they wrap a specific library or infrastructure concern directly, and are tested either against that concern directly (e.g., Testcontainers Redis, as already used in `service-auth`) or left untested at the wiring level while the decision logic behind them (where one exists) is unit tested through the interfaces above.

## Consequences

**Positive:**
- Avoids building an abstraction layer with no second implementation and no independent domain logic behind it — keeps the Gateway's codebase proportional to its actual complexity.
- The two genuine business decisions (rate limit, route authorization) remain independently testable without live infrastructure, consistent with the fake-adapter testing style already established in `service-auth`.
- Faster to read and navigate for a Gateway of this size — a new contributor (or the project owner returning to it later) doesn't need to trace through port/adapter indirection to find where the actual Resilience4j or WebClient call happens.

**Negative / Accepted limitations:**
- If the Gateway's responsibilities grow to include genuine domain logic beyond rate limiting and route authorization (e.g., request transformation rules, complex multi-tenant routing logic), this decision should be revisited — more interfaces may become justified at that point.
- The flat package structure means less enforced separation than `service-auth`'s layering; discipline to keep infrastructure code out of `RateLimitDecision`/`RouteAuthorizationDecision` implementations relies on review, not package boundaries.

## Alternatives Considered

| Alternative | Reason for rejection |
|---|---|
| Full hexagonal architecture (matching `service-auth`) | Would wrap circuit breaker, load balancer, and forwarding logic in ports with a single adapter each — abstraction with no second implementation and no isolated domain logic to protect |
| No abstraction at all, everything as plain classes | Would lose the ability to unit-test the rate limit and route authorization decisions independently of Redis/config, which is a concrete, near-term testing need for those two specific pieces |

## Revisit Triggers

This decision should be revisited if:
- The Gateway is asked to implement genuine business rules beyond rate limiting and route authorization (e.g., tenant-specific routing policy, request/response transformation logic).
- A third interface candidate emerges where technology-independent decision logic and a real testing payoff both exist.
