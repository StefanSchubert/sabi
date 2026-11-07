# Research: OpenID Connect Login via Google (sabi-150)

**Phase**: 0 — Pre-Implementation Research  
**Date**: 2026-03-26  
**Status**: Complete

---

## 1. Google OIDC Discovery & Endpoints

Google publishes its OIDC configuration via the Discovery Document:

```
https://accounts.google.com/.well-known/openid-configuration
```

| Endpoint | URL | Purpose |
|----------|-----|---------|
| Authorization | `https://accounts.google.com/o/oauth2/v2/auth` | Redirect user to Google login |
| Token Exchange | `https://oauth2.googleapis.com/token` | Exchange authorization code for tokens |
| UserInfo | `https://openidconnect.googleapis.com/v1/userinfo` | Fetch user profile (fallback) |
| JWKS (public keys) | `https://www.googleapis.com/oauth2/v3/certs` | Verify ID token signature |
| Revocation | `https://oauth2.googleapis.com/revoke` | Revoke tokens on logout |
| Issuer (iss claim) | `https://accounts.google.com` | Must match in ID token validation |

**Spring Security** reads the Discovery Document automatically via:
```yaml
spring.security.oauth2.client.provider.google.issuer-uri: https://accounts.google.com
```
No manual endpoint configuration needed.

---

## 2. Required OAuth2 Scopes

| Scope | Claim(s) returned | Purpose |
|-------|-------------------|---------|
| `openid` | `sub`, `iss`, `aud`, `exp`, `iat` | Mandatory for OIDC; enables ID token |
| `email` | `email`, `email_verified` | Account matching / provisioning |
| `profile` | `name`, `given_name`, `family_name`, `locale`, `picture` | Username for auto-provisioned accounts; locale default |

Minimum required: `openid email`  
Recommended: `openid email profile`

---

## 3. Google ID Token Claims

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | Immutable Google user ID — primary key for `oidc_provider_link.provider_subject` |
| `email` | String | User's email — used for account matching (FR-9) |
| `email_verified` | Boolean | Must be `true`; unverified emails are rejected |
| `name` | String | Full display name — used for `username` on auto-provisioning |
| `given_name` | String | First name |
| `locale` | String | BCP 47 locale tag (e.g. `de`, `en`) — used for `language` setting on auto-provisioning |
| `iss` | String | Must equal `https://accounts.google.com` (C-4) |
| `aud` | String | Must match Sabi's `client_id` (C-4) |
| `exp` | Long | Unix timestamp; token must not be expired |
| `nonce` | String | Must match the nonce sent in authorization request (C-6) |

---

## 4. Existing Authentication Flow (Baseline)

```
Browser → sabi-webclient (JSF/PrimeFaces)
              │  POST /api/auth/login  (AccountCredentialsTo)
              ▼
         sabi-server (Spring Boot REST)
              │  JWTLoginFilter intercepts
              │  SabiDoorKeeper.signIn()
              │  UserService.signIn(email, password)
              ▼
         TokenAuthenticationService.addAuthentication()
              │  JWT signed with HMAC (auth0 java-jwt)
              │  Header: Authorization: Bearer <token>
              ▼
         Webclient stores token → subsequent API calls include it
```

**Relevant classes**:
- `JWTLoginFilter` — intercepts `POST /api/auth/login`
- `TokenAuthenticationService` — creates/validates JWT; `createAuthorizationTokenFor(email)`
- `SabiDoorKeeper` (webclient) — stores JWT in session, adds to all outgoing requests
- `AppConfig` — holds `accessToken.SECRET` and `accessToken.TTL`

**Key insight**: The JWT subject (`sub`) is the user's **email address**. The new OIDC flow must produce an identical JWT after OIDC authentication — reusing `TokenAuthenticationService.createAuthorizationTokenFor(email)`.

---

## 5. Existing Registration Flow (to be bypassed for OIDC)

```
POST /api/auth/register  →  CaptchaAdapter.validate()
                         →  UserService.registerNewUser()
                         →  NotificationService.sendEmailConfirmation()
                         →  User must click confirmation link
```

For OIDC users: Google guarantees `email_verified=true`, so **no Captcha and no email confirmation** are needed. The new `provisionOidcUser()` service method creates the account directly in `ACTIVE` state.

---

## 6. Spring Security OAuth2 Client — Configuration Pattern

### sabi-webclient `application.properties`
```properties
# Google OIDC Provider (auto-configured via Discovery Document)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_OIDC_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_OIDC_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com
```

**Secrets**: `GOOGLE_OIDC_CLIENT_ID` and `GOOGLE_OIDC_CLIENT_SECRET` are injected as environment variables — never committed to the repository (C-2).

### Spring Security auto-provides
- `state` parameter generation and validation (C-3)
- `nonce` parameter generation and validation (C-6) — when `scope` includes `openid`
- ID token signature verification via Google JWKS endpoint
- `iss` and `aud` claim validation (C-4)

---

## 7. Flyway Version Analysis

Current latest migration: `V1_3_0_4__i18nUnit.sql`  
New migrations required:

| File | Directory | Content |
|------|-----------|---------|
| `V1_4_0_1__addOidcProviderLinkTable.sql` | `version1_4_0/` | New `oidc_provider_link` table |
| `V1_4_0_2__addOidcManagedFlagToUsers.sql` | `version1_4_0/` | `oidc_managed` column on `users` table |

---

## 8. IPv6 / DS-Lite Reachability Assessment

| Aspect | Assessment |
|--------|-----------|
| Google App Registration | DNS hostname (Strato DynDNS) available → used as redirect URI. No bare IPv6 needed. ✅ |
| HTTPS requirement | Already in place for production. ✅ |
| Google callback to IPv6-only host | Google infrastructure has full IPv6 support (AAAA on `accounts.google.com`). Expected to work. ⚠️ Validate during E2E test. |
| Local development | Use `http://localhost:8080` as redirect URI in Google Cloud Console (dev credentials, separate from prod). ✅ |

---

## 9. Extensibility for Apple and Microsoft (Future Iterations)

| Provider | Discovery Document | Notes |
|----------|--------------------|-------|
| Apple | `https://appleid.apple.com/.well-known/openid-configuration` | Requires special JWT client secret generation; sabi-152 |
| Microsoft | `https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration` | Tenant-based; sabi-152 |

The `OidcProviderLink.provider` enum (`GOOGLE`, `APPLE`, `MICROSOFT`) is designed for this. Adding a new provider requires only new Spring Security registration config + the enum value — no model changes (SC-8).

---

*Phase 0 complete. Proceed to data-model.md and contracts/oidc-api.md.*

