# API Contract: OIDC Authentication Endpoint (sabi-150)

**Phase**: 1 — Design  
**Date**: 2026-03-26  
**Module**: `sabi-server`  
**Status**: Complete

---

## Overview

The OIDC authentication endpoint is the single integration point between `sabi-webclient` and `sabi-server` for Google login. After the webclient completes the OAuth2 Authorization Code Flow with Google, it forwards the raw Google ID token to this endpoint. The backend validates, provisions or links the account, and returns a Sabi JWT.

---

## Endpoint: POST /api/auth/oidc/google

### Summary
Exchange a validated Google ID token for a Sabi session token.

### Security
- **No Sabi JWT required** — this is a public endpoint (`permitAll` in `WebSecurityConfig`)
- ID token validation is performed server-side (signature via Google JWKS, `iss`, `aud`, `nonce`, `exp`)

### Request

```
POST /api/auth/oidc/google
Content-Type: application/json
```

**Request Body** (`OidcLoginRequestTo`):

```json
{
  "idToken": "<raw Google ID token JWT string>",
  "provider": "GOOGLE"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `idToken` | String | ✅ | Raw ID token JWT as returned by Google's token endpoint. The webclient obtains this from Spring Security's `OAuth2AuthorizedClient`. |
| `provider` | String | ✅ | Always `"GOOGLE"` for this iteration. Future values: `"APPLE"`, `"MICROSOFT"`. |

---

### Response — Success (200 OK)

```json
{
  "token": "<Sabi JWT>",
  "email": "user@example.com",
  "username": "Max Mustermann",
  "provisioned": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `token` | String | Sabi JWT — identical format to password-login JWT. Added to `Authorization: Bearer` header by webclient. |
| `email` | String | The verified email address from the Google ID token. |
| `username` | String | The Sabi username (existing or newly provisioned). |
| `provisioned` | Boolean | `true` if a new Sabi account was auto-created; `false` if an existing account was used/linked. |

---

### Response — Error Cases

| HTTP Status | Condition | Body |
|-------------|-----------|------|
| `400 Bad Request` | Missing or malformed request body | `{ "error": "INVALID_REQUEST" }` |
| `401 Unauthorized` | ID token invalid: signature, `iss`, `aud`, `exp`, or `nonce` check failed | `{ "error": "INVALID_ID_TOKEN", "detail": "<reason>" }` |
| `401 Unauthorized` | `email_verified` is `false` in Google ID token | `{ "error": "EMAIL_NOT_VERIFIED" }` |
| `409 Conflict` | Email already linked to a **different** Google `sub` (edge case: account takeover attempt) | `{ "error": "IDENTITY_CONFLICT" }` |
| `423 Locked` | Matching Sabi account exists but is locked/disabled (OQ-3 resolution) | `{ "error": "ACCOUNT_LOCKED" }` |
| `500 Internal Server Error` | Unexpected failure during provisioning | `{ "error": "PROVISIONING_FAILED" }` |

---

### Flow Diagram

```
sabi-webclient                          sabi-server
     │                                       │
     │  POST /api/auth/oidc/google            │
     │  { idToken: "<raw JWT>" }             │
     │──────────────────────────────────────►│
     │                                       │  1. Parse ID token header (kid)
     │                                       │  2. Fetch Google JWKS (cached)
     │                                       │  3. Verify signature
     │                                       │  4. Validate iss, aud, exp, nonce
     │                                       │  5. Check email_verified == true
     │                                       │  6. Lookup OidcProviderLink by (GOOGLE, sub)
     │                                       │     ├─ Found → load UserEntity
     │                                       │     └─ Not found → lookup UserEntity by email
     │                                       │         ├─ Found → link OIDC identity (FR-9)
     │                                       │         └─ Not found → provision new user (FR-8)
     │                                       │  7. Check account not locked (FR locked → 423)
     │                                       │  8. Issue Sabi JWT via TokenAuthenticationService
     │                                       │  9. Audit log event (FR-15)
     │◄──────────────────────────────────────│
     │  200 OK  { token, email, username,    │
     │            provisioned }              │
     │                                       │
     │  Store JWT in SabiDoorKeeper session  │
     │  Redirect to dashboard                │
```

---

## Webclient → Backend Integration Detail

### How the Webclient obtains the ID token

Spring Security OAuth2 Client completes the Authorization Code Flow automatically. In `SabiOidcSuccessHandler.onAuthenticationSuccess()`:

```java
// Pseudo-code — actual implementation in tasks
OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
String rawIdToken = oidcUser.getIdToken().getTokenValue();  // raw JWT string
// POST to sabi-server /api/auth/oidc/google with { idToken: rawIdToken, provider: "GOOGLE" }
```

### Re-authentication on JWT Expiry (FR-10)

When the Sabi JWT expires for an OIDC-authenticated user, the webclient detects the 401 response and triggers a new OIDC authorization flow via Spring Security's `OAuth2AuthorizationRequestRedirectFilter` — transparent to the user if a valid Google session exists.

---

## Audit Log Events (FR-15)

| Event | Log Level | Example Message |
|-------|-----------|-----------------|
| Successful login (existing account) | INFO | `OIDC_LOGIN_SUCCESS provider=GOOGLE userId=42 provisioned=false` |
| Successful login (new account) | INFO | `OIDC_LOGIN_SUCCESS provider=GOOGLE userId=99 provisioned=true` |
| Successful login (account linked) | INFO | `OIDC_LINK_CREATED provider=GOOGLE userId=42` |
| ID token validation failure | WARN | `OIDC_LOGIN_FAILED provider=GOOGLE reason=INVALID_SIGNATURE` |
| Invalid state / CSRF attempt | WARN | `OIDC_CSRF_DETECTED provider=GOOGLE sourceIp=<ip>` |
| Account locked | WARN | `OIDC_LOGIN_REJECTED provider=GOOGLE reason=ACCOUNT_LOCKED userId=42` |

> **No plain-text PII** in log output. Email addresses and Google `sub` values are never logged in the clear.

---

## OpenAPI Annotation Target

Controller: `de.bluewhale.sabi.rest.controller.OidcAuthController`  
Annotation: `@Tag(name = "OIDC Authentication")`  
SpringDoc will expose this under the existing `/v3/api-docs` endpoint.

---

*Contract complete. Proceed to speckit.tasks for implementation task breakdown.*

