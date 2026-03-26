# Feature Specification: OpenID Connect Login via Google

**Feature ID**: sabi-150  
**Branch**: feature/sabi-150  
**Status**: Draft  
**Created**: 2026-03-26  
**Owner**: Stefan Schubert  

---

## Overview

### Problem Statement

New users who discover Sabi face a friction barrier: they must complete a full registration process — including Captcha verification and email validation — before they can even try the application. This reduces the number of people who actually give Sabi a try.

### Proposed Solution

Allow users to authenticate with their existing Google account via OpenID Connect (OIDC). A single click replaces the entire registration and login workflow for users who already have a Google account. Apple and Microsoft are planned as follow-up iterations; the architecture must support all three providers.

### Business Value

- **Lower barrier to entry**: New users can access Sabi in one click, without creating yet another account.
- **Improved security posture**: Delegating credential management to a trusted identity provider (Google) reduces Sabi's attack surface for credential-based attacks.
- **User trust**: Familiar "Login with Google" pattern builds confidence for users who are cautious about sharing passwords with lesser-known services.

---

## Scope

### In Scope

- "Login with Google" button on the existing login page
- Automatic account provisioning for first-time Google OIDC users (no manual registration required)
- Account linking for existing Sabi users whose email address matches their Google account
- Bypassing Captcha verification for OIDC-authenticated users
- Bypassing email validation for OIDC-authenticated users (Google guarantees email ownership)
- Session establishment using Sabi's existing access token mechanism after successful OIDC authentication
- Google App Registration (OAuth2 / OIDC client credentials configuration)
- Documentation of IPv6 reachability considerations for Google App Registration
- Architecture that supports adding Apple and Microsoft as additional providers with minimal effort

### Out of Scope (this iteration)

- Apple ID login (planned: sabi-151)
- Microsoft / Azure AD login (planned: sabi-152)
- Account merging UI for users with both a local and a Google account but different email addresses
- Disabling the existing username/password login
- Self-service provider management in the user profile

---

## User Stories

### US-1: New User — First-Time Login with Google

> *As a new visitor to Sabi, I want to log in with my Google account so that I can start using the application immediately without filling in a registration form.*

**Acceptance Criteria**:
- The login page displays a clearly labelled "Login with Google" button alongside the existing username/password form.
- Clicking the button redirects me to Google's sign-in page.
- After successfully authenticating with Google, I am redirected back to Sabi and land on my personal dashboard, fully logged in.
- No registration form, Captcha, or email confirmation is required.
- My Sabi account is created automatically in the background.

### US-2: Existing User — Login with Google (Email Match)

> *As an existing Sabi user, I want to be able to use my Google account to log in, provided my Google email address matches the one I registered with, so that I don't end up with a duplicate account.*

**Acceptance Criteria**:
- If my Google email address matches an existing Sabi account, I am logged into that account.
- All my existing data (aquariums, measurements, fish, etc.) is preserved and accessible.
- I am not prompted to create a new account.

### US-3: Returning Google OIDC User

> *As a user who previously logged in via Google, I want subsequent logins with Google to work without any additional steps, so that the experience remains seamless.*

**Acceptance Criteria**:
- On subsequent logins via Google, I am recognized and authenticated immediately.
- No re-provisioning or re-confirmation steps are triggered.

### US-4: Application Owner — Google App Registration

> *As the Sabi application owner, I need to register the Sabi application with Google so that the OIDC flow is technically authorized by Google.*

**Acceptance Criteria**:
- The Sabi backend is registered as an OAuth2/OIDC client in Google Cloud Console.
- The required callback/redirect URL is registered with Google.
- The registration is valid for Sabi's IPv6-only reachable production environment (see Constraint C-1).
- Client ID and Client Secret are stored securely in the application configuration (not committed to the repository).

---

## User Scenarios & Testing

### Scenario 1: Happy Path — New User, First OIDC Login

```
Given  a visitor is on the Sabi login page
  And  they have a valid Google account
 When  they click "Login with Google"
  And  they authenticate successfully with their Google account
 Then  a new Sabi account is provisioned automatically
  And  the user is redirected to the personal dashboard
  And  no Captcha, registration form, or email validation appears
```

### Scenario 2: Happy Path — Existing User, Email Match

```
Given  a registered Sabi user is on the login page
  And  their Sabi email matches their Google account email
 When  they click "Login with Google"
  And  they authenticate successfully with Google
 Then  they are logged into their existing Sabi account
  And  all their data is intact and accessible
```

### Scenario 3: Happy Path — Returning OIDC User

```
Given  a user has previously logged in via Google
 When  they visit the login page and click "Login with Google"
  And  they authenticate with Google (Google may skip re-auth if session is active)
 Then  they are logged into Sabi immediately
```

### Scenario 4: Google Authentication Fails or is Cancelled

```
Given  a user clicks "Login with Google"
 When  they cancel the Google authentication or Google returns an error
 Then  they are redirected back to the Sabi login page
  And  an appropriate error message is displayed
  And  the username/password login form remains available
```

### Scenario 5: Google Returns Email Already Linked to Different Account

```
Given  a user authenticates with Google
  And  the Google email matches an existing Sabi account that was already linked to a different Google sub (identity)
 When  the authentication callback is processed
 Then  the existing linked account is used for login
  And  no duplicate account is created
```

### Scenario 6: Google Service Unavailable

```
Given  a user clicks "Login with Google"
 When  Google's authentication service is unreachable
 Then  the user sees an informative error message
  And  the standard username/password login remains functional as a fallback
```

### Edge Cases

- User's Google account email is already registered in Sabi but as an unvalidated account → the OIDC login should treat the email match as sufficient and log the user in, marking the account as validated.
- User has a Google account with an email that has never been seen by Sabi → new account is auto-provisioned.
- Google returns a different email on a subsequent login for the same Google user (edge case; guard against account takeover by also matching the Google subject identifier).

---

## Functional Requirements

### FR-1: Login Dialog Enhancement

The login page **must** display a "Login with Google" button that is visually distinct from the username/password form and clearly communicates its purpose.

### FR-2: OIDC Redirect Initiation

When a user clicks "Login with Google", the application **must** initiate an OIDC authorization code flow by redirecting the user to Google's authorization endpoint.

### FR-3: Required Google OIDC Endpoints

The application **must** integrate with the following Google OIDC endpoints (discoverable via the standard OpenID Connect discovery document at `https://accounts.google.com/.well-known/openid-configuration`):

| Endpoint | Purpose |
|----------|---------|
| **Authorization Endpoint** (`https://accounts.google.com/o/oauth2/v2/auth`) | Initiates the user authentication flow; the user is redirected here |
| **Token Endpoint** (`https://oauth2.googleapis.com/token`) | Exchanges the authorization code for ID token and access token |
| **UserInfo Endpoint** (`https://openidconnect.googleapis.com/v1/userinfo`) | Retrieves verified user profile data (email, name) |
| **JWKS Endpoint** (`https://www.googleapis.com/oauth2/v3/certs`) | Provides Google's public keys to verify the ID token signature |

### FR-4: Requested OIDC Scopes

The application **must** request the following scopes from Google:

| Scope | Purpose |
|-------|---------|
| `openid` | Signals that this is an OIDC request; enables the ID token |
| `email` | Provides the user's verified email address for account matching / provisioning |
| `profile` | Provides display name and locale for personalisation |

### FR-5: Callback / Redirect URI Handling

The application **must** handle Google's authorization callback at a dedicated, registered endpoint. The callback URL **must** be registered in Google Cloud Console. The application **must** validate the `state` parameter to prevent CSRF attacks on the OAuth2 flow.

### FR-6: ID Token Validation

The application **must** cryptographically validate the Google ID token using Google's public JWKS keys before trusting any claims it contains.

### FR-7: Account Matching — Email-Based

After successful Google authentication, the application **must** look up the Sabi user database using the verified email address from the Google ID token (`email` claim). The email address from Google is considered pre-verified.

### FR-8: Account Provisioning — New Users

If no Sabi account exists for the Google-supplied email address, the application **must** automatically create a new user account with the following rules:
- Email address taken from the Google ID token (`email` claim)
- Display name taken from Google's `name` claim (if available)
- The account is immediately activated (no email validation step)
- No Captcha check is performed
- No password is required; the account is flagged as an OIDC-managed account
- The Google subject identifier (`sub` claim) **must** be stored for future identity matching

### FR-9: Account Provisioning — Existing Users

If a Sabi account already exists with the matching email address:
- The user is logged into the existing account
- The Google subject identifier (`sub` claim) is associated with the account (if not already done)
- If the existing account was unvalidated (pending email confirmation), it is marked as validated
- `oidcManaged` **remains `false`** — the existing password-based login is preserved; Google is added as an **additional** login option alongside the existing password login. The user retains full choice of how to authenticate.

### FR-10: Sabi Session Token Issuance

After successful OIDC authentication and account resolution, the application **must** issue a Sabi session token (using the same token mechanism as the existing username/password login) so that subsequent API calls are authorized identically regardless of how the user authenticated.

**JWT expiry / re-authentication**: When a Sabi session token issued to an OIDC user expires, the application **must** automatically redirect the user back to the Google OIDC authorization flow — without showing the Sabi login dialog first. If the user still has an active Google session, this results in a seamless Single Sign-On re-authentication (zero visible interruption). If Google requires user interaction (e.g., session expired), the Google login prompt is shown instead.

### FR-11: Captcha Bypass for OIDC Users

Users who authenticate via OIDC **must not** be subjected to Captcha verification at any point in the login or auto-provisioning flow.

### FR-12: Extensibility for Additional Providers

The OIDC integration **must** be designed so that Apple and Microsoft can be added as additional identity providers with changes isolated to provider-specific configuration, without requiring structural changes to the user account model or session management.

### FR-13: Google App Registration

The application owner **must** register Sabi as an OAuth2/OIDC client with Google Cloud Console. The registration **must** include:
- A descriptive application name
- The authorized callback/redirect URI(s) for all deployment environments
- The required OAuth2 scopes (`openid`, `email`, `profile`)

### FR-14: IPv6 Reachability Consideration

The production environment operates under **DS-Lite** (Dual-Stack Lite): the server is reachable **only via IPv6**, not via native IPv4. A **DNS hostname is available** through Strato DynDNS and will be used as the redirect URI in Google Cloud Console — no bare IPv6 address needs to be registered. Google requires HTTPS and a valid domain name for production redirect URIs; both conditions are met.

The residual risk is that Google's OAuth2 callback servers must be able to initiate a connection to an IPv6-only host. Google's infrastructure fully supports IPv6 (`accounts.google.com` has AAAA records), making this expected to work. The end-to-end flow must be validated after App Registration is complete (see OQ-1).

### FR-15: Security Audit Logging for OIDC Events

The following OIDC-related events **must** be written to the application's security audit log:

| Event | Log Level | Mandatory Fields |
|-------|-----------|-----------------|
| Successful OIDC login (existing account) | INFO | Timestamp, provider (`GOOGLE`), anonymised user reference |
| Successful OIDC login + new account auto-provisioned | INFO | Timestamp, provider, anonymised user reference |
| Successful OIDC login + existing account linked | INFO | Timestamp, provider, anonymised user reference |
| Failed OIDC authentication (token validation failure, provider error) | WARN | Timestamp, provider, error reason (no personal data) |
| OIDC callback received with invalid `state` (CSRF attempt) | WARN | Timestamp, provider, source IP |

Personal data (e.g., email, Google `sub`) **must not** appear in plain text in log output. A hashed or anonymised reference to the user account is sufficient for forensic traceability.

---

## Non-Functional Requirements (ISO 25010)

| ISO 25010 Quality Characteristic | Requirement / Constraint for this Feature |
|-----------------------------------|-------------------------------------------|
| Functional Suitability | All 14 functional requirements (FR-1 – FR-14) must be met without gaps. Account matching and auto-provisioning must produce no duplicate accounts. |
| Performance Efficiency | The OIDC redirect-and-callback round trip must not add perceptible latency beyond the Google authentication itself. The token-issuance step after OIDC login must complete within the same time budget as the existing password-based login. |
| Compatibility | The OIDC integration must not break the existing username/password login. The session token issued after OIDC login must be compatible with all existing API endpoints. |
| Usability | The "Login with Google" button must be clearly labelled and visually accessible on the existing login page. New users must complete their first OIDC login in under 30 seconds (see SC-1). WCAG 2.1 AA must be maintained. |
| Reliability | If Google's OIDC service is temporarily unavailable, the existing username/password login must remain fully functional (graceful degradation, see Scenario 6). |
| Security | Google ID tokens must be cryptographically validated (signature, issuer, audience) before any claims are trusted (C-3, C-4). The client secret obtained from Google must never be committed to the repository (C-2). The CSRF state parameter must be validated on every callback (C-3). |
| Maintainability | Adding a second OIDC provider (Apple, Microsoft) must require no changes to the user account model or session management — only provider-specific configuration (SC-8). Database changes must be applied via the existing migration tooling. |
| Portability | The OIDC integration must run on the existing ARM-based production environment without platform-specific adjustments. Docker and Ansible deployment artefacts must remain compatible. |

---

## Success Criteria

| # | Criterion | Measurement |
|---|-----------|-------------|
| SC-1 | New users can complete their first login via Google in under 30 seconds from clicking the button | Manual user test; stopwatch from button click to dashboard display |
| SC-2 | No registration form, Captcha, or email confirmation is shown to OIDC-authenticated users | Functional test: automated scenario covering FR-8 and FR-11 |
| SC-3 | Existing users with a matching email are correctly recognized; no duplicate accounts are created | Data integrity test: verify single user record after OIDC login for a pre-existing account |
| SC-4 | 100% of Google ID tokens are validated before any claims are trusted | Security review: verify validation step is present and cannot be bypassed |
| SC-5 | OIDC-authenticated users receive a valid Sabi session token equivalent to that issued via password login | Functional test: OIDC login followed by authenticated API request returns HTTP 200 |
| SC-6 | Username/password login continues to function correctly for users who do not use Google OIDC | Regression test: existing login scenarios pass without modification |
| SC-7 | Google App Registration is accepted by Google Cloud Console with the production redirect URI (IPv6 environment) | Manual verification: successful test OIDC flow against the production environment |
| SC-8 | Adding a second OIDC provider (Apple or Microsoft) requires no changes to the user account model or session management | Architecture review: provider-specific changes are limited to configuration |

---

## Key Entities

### OidcProviderLink (new)

Represents the association between a Sabi user account and an external OIDC identity.

| Attribute | Description |
|-----------|-------------|
| `id` | Internal identifier |
| `userId` | Reference to the Sabi user account (`ON DELETE CASCADE` — the link is automatically removed when the user account is deleted, ensuring GDPR-compliant data removal) |
| `provider` | Name of the identity provider (e.g., `GOOGLE`, `APPLE`, `MICROSOFT`) |
| `providerSubject` | The subject identifier (`sub`) from the provider's ID token — uniquely identifies the user at the provider. Classified as personal data under GDPR Art. 4; deleted automatically via cascade when the user account is removed. |
| `linkedAt` | Timestamp when the link was established |

### UserAccount (modified)

The existing user account entity requires a new flag to distinguish OIDC-managed accounts from password-managed accounts.

| New Attribute | Description |
|---------------|-------------|
| `oidcManaged` | Boolean flag; `true` **only** if the account was **auto-provisioned** via OIDC (new user, no prior Sabi account) and has no local password set. `false` for all existing accounts that are linked to a Google identity — those retain their password-based login. |

---

## Dependencies & Assumptions

### Dependencies

- **Google Cloud Console access**: The application owner must have access to create an OAuth2/OIDC client in Google Cloud Console.
- **HTTPS on the Sabi backend**: Google requires HTTPS for production redirect URIs. It is assumed Sabi's production environment already uses HTTPS.
- **OIDC client capability**: The backend already includes the necessary OAuth2/OIDC client library support — no additional third-party dependency is required.

### Assumptions

1. **Formal requirements already met**: Terms of use, privacy policy, and GDPR considerations for using Google as an identity provider are already prepared (as stated by the product owner).
2. **Email as primary matching key**: The email address from Google's ID token is trusted for account matching. If a Sabi account was manually created with the same email by a different person (unlikely but theoretically possible), the first OIDC login will link to it.
3. **Google subject identifier stored**: To support the edge case where a user changes their Google email, the `sub` claim (immutable at Google) is stored and can serve as a fallback matching key in a future iteration.
4. **Language/locale**: On auto-provisioning, the user's locale preference is derived from Google's `locale` claim if available; otherwise the application default is used.
5. **No password for OIDC accounts**: Auto-provisioned accounts do not have a usable local password. If the user later wants to set one (e.g., to also enable password login), that is a future feature.
6. **DNS hostname via Strato DynDNS**: The application owner has a DNS hostname provided by Strato DynDNS. This hostname — not a bare IPv6 address — is used as the redirect URI in Google Cloud Console. This is fully acceptable to Google and removes the IPv6-address-registration concern entirely. The residual question is whether Google's OAuth2 callback servers can reach a DS-Lite host (IPv6-only, no native IPv4): Google's infrastructure fully supports IPv6 (accounts.google.com has AAAA records), so callback delivery to an IPv6-only host is expected to work. This must be validated during App Registration and the first end-to-end test.

---

## Constraints

| ID | Constraint |
|----|-----------|
| C-1 | Google requires HTTPS for all production redirect URIs |
| C-2 | The client secret obtained from Google Cloud Console **must not** be stored in the source code repository |
| C-3 | The OIDC `state` parameter **must** be validated to protect against CSRF attacks |
| C-4 | The `iss` (issuer) and `aud` (audience) claims of the Google ID token **must** be validated |
| C-5 | Only the Google OIDC provider is in scope for this iteration; the architecture must not prevent adding Apple and Microsoft later |
| C-6 | A `nonce` parameter **must** be included in the OIDC Authorization Request and validated in the returned ID Token to protect against token-replay attacks (OIDC Core Spec §3.1.2.1) |

---

## Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| OQ-1 | **DS-Lite / IPv6-only reachability**: A Strato DynDNS hostname is available and will be used as the redirect URI (no bare IPv6 address needed — Google accepts DNS hostnames without issue). The server is DS-Lite, meaning it is **only reachable via IPv6** (no native IPv4). Google's OAuth2 infrastructure supports IPv6 (AAAA records on `accounts.google.com`), so callbacks to an IPv6-only host are expected to work. **Must be validated** during App Registration and the first end-to-end test. | Stefan | Partially Resolved — confirm during E2E test |
| OQ-2 | Should the "Login with Google" callback be handled by the backend (sabi-server) or the frontend webclient? The current architecture has the webclient as a separate Spring Boot app that calls the backend via REST. | Stefan | Open — architectural decision needed before implementation |
| OQ-3 | If an existing user logs in via Google but their Sabi account is locked, should the OIDC flow honour the lock or bypass it? | Stefan | Open |

---

*This specification was created for sabi-150. Next step: `/speckit.plan` to break this into implementation tasks.*

