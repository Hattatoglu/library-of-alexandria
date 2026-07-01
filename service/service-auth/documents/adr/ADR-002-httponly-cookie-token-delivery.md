# ADR-002: HttpOnly Cookie for Token Delivery

| Field        | Value                          |
|--------------|--------------------------------|
| **ID**       | ADR-002                        |
| **Status**   | Accepted                       |
| **Date**     | 2025-06-29                     |
| **Author**   | Eyaz Hattatoğlu                |
| **Deciders** | Eyaz Hattatoğlu                |
| **Tags**     | security, cookies, xss, csrf   |

---

## Context

After successful authentication, `service-auth` issues an access token and a refresh token.
These tokens must be transmitted to the client and re-attached to subsequent requests. Three
common delivery mechanisms exist in the industry:

1. JSON response body, stored client-side in `localStorage` or `sessionStorage`.
2. JSON response body, stored client-side in JavaScript memory (in-memory only).
3. `Set-Cookie` header with `HttpOnly` flag, stored by the browser, inaccessible to JavaScript.

The primary threat model under consideration is **Cross-Site Scripting (XSS)**. If a single
vulnerable dependency or unsanitized user input allows script injection, any token stored in
`localStorage` or accessible via `document.cookie` can be exfiltrated by the attacker.

---

## Decision

We will deliver both access and refresh tokens via **`Set-Cookie` headers with `HttpOnly`,
`Secure`, and `SameSite=Strict` flags**, implemented in `CookieProvider`.

```java
ResponseCookie.from(name, value)
    .httpOnly(true)
    .secure(cookieProperties.secure())
    .sameSite(cookieProperties.sameSite())
    .domain(cookieProperties.domain())
    .path("/")
    .maxAge(Duration.ofSeconds(maxAgeSeconds))
    .build();
```

- `access_token`: max-age = 15 minutes (`JWT_ACCESS_TOKEN_EXP`).
- `refresh_token`: max-age = 7 days (`JWT_REFRESH_TOKEN_EXP`).
- Both cookies are scoped to `path=/` and the configured `COOKIE_DOMAIN`.
- JavaScript on the client **cannot read, write, or delete these cookies** — the browser
  enforces this at the `HttpOnly` flag level.
- The browser automatically attaches cookies to same-origin requests; no client-side token
  management code is required.

---

## Alternatives Considered

### Option A: JSON Response Body + `localStorage`

Most commonly seen in tutorials and early-stage SPAs.

**Rejected.** Any successful XSS payload can read `localStorage` synchronously and exfiltrate
both tokens in a single line of injected JavaScript. Given that the access token is short-lived
this is a smaller risk, but the refresh token has a 7-day lifetime — its exposure is a critical
compromise of the user's session.

### Option B: JSON Response Body + In-Memory JavaScript Variable

Tokens are kept only in application memory (e.g. a React context or Redux store), never
persisted to `localStorage`.

**Rejected as the sole mechanism**, though it reduces persistent exposure compared to
`localStorage`. It still requires the access token to pass through JavaScript-readable memory,
meaning an active XSS payload running at the same time as a legitimate session can still read
the token from memory. It also requires re-authentication on every page refresh unless paired
with a refresh-token flow — which reintroduces the storage problem for the refresh token.

### Option C: HttpOnly Cookie (Selected)

**Accepted.** Tokens are never exposed to JavaScript under any circumstance, including a
successful XSS payload. The trade-off is that this introduces CSRF as a new attack surface,
addressed below.

---

## Consequences

### Positive

- **XSS cannot exfiltrate tokens.** Even a fully successful script injection cannot read
  `HttpOnly` cookies. This is the single strongest mitigation available against the most common
  web application vulnerability class.
- Client-side code is simplified — no manual `Authorization` header management, no token
  refresh orchestration in JavaScript. The browser handles attachment automatically.
- `Secure` flag ensures cookies are never transmitted over plaintext HTTP.

### Negative — CSRF Exposure

Cookies are automatically attached by the browser to *any* request to the cookie's domain,
including requests initiated by a malicious third-party site. This reintroduces Cross-Site
Request Forgery as an active threat.

**Mitigation:** `SameSite=Strict` is set on both cookies. This instructs the browser to omit
the cookie entirely on cross-site requests, including top-level navigations from external links.
Combined with CSRF being primarily exploitable on state-changing `GET` requests (which this API
does not use for authentication actions — all auth mutations are `POST`/`PATCH`), the practical
CSRF surface is significantly reduced.

**Accepted residual risk:** `SameSite=Strict` does not protect against CSRF originating from
a *subdomain* of the same site if that subdomain is compromised. This is accepted as out of
scope for the current threat model; subdomain isolation is the responsibility of infrastructure
hardening, not the authentication service.

### Negative — Cross-Origin Client Limitations

`HttpOnly` cookies require the client and `service-auth` (via `api-gateway`) to share a
registered domain relationship compatible with the configured `SameSite` policy. This is a
known constraint for:

- **Mobile native clients** (iOS/Android) — these do not have a cookie jar shared with a
  browser session in the same way. A future mobile client will require a separate token
  delivery mechanism (e.g. Authorization header with secure OS-level keychain storage),
  documented as a follow-up ADR when mobile support is scoped.
- **Third-party origin SPAs** — if a frontend is ever served from a genuinely different
  registrable domain than the API, `SameSite=Strict` will block the cookie entirely. This is
  intentional and by design; cross-origin credentialed requests are explicitly out of scope
  for the current architecture.

---

## Related Decisions

- ADR-001: Asymmetric JWT (RS256) — defines what is being delivered via this mechanism.
- ADR-003: Refresh Token Rotation Strategy — defines the lifecycle of the long-lived cookie.
