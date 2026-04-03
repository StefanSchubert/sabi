# Specification Quality Checklist: OpenID Connect Login via Google

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-03-26
**Feature**: [spec.md](../spec.md)

---

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

> **Note on FR-3 (OIDC Endpoints)**: The Google OIDC endpoint URLs listed in FR-3 are
> external, Google-managed addresses -- not Sabi implementation decisions. They are documented
> here for operational completeness as explicitly requested by the product owner.

---

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

---

## Open Items

| # | Item | Status |
|---|------|--------|
| OQ-1 | DNS hostname (Strato DynDNS) confirmed available → used as redirect URI. DS-Lite (IPv6-only) residual risk: must validate Google callback reaches IPv6-only host during E2E test | Partially Resolved |
| OQ-2 | OIDC callback handled by backend vs. frontend webclient | Open — architectural decision needed |
| OQ-3 | Locked account behaviour during OIDC flow | Open — product decision needed |

---

## Clarifications Resolved (2026-03-26)

| # | Question | Decision |
|---|----------|----------|
| CL-1 | OIDC `nonce` parameter (token-replay protection) | **A** — `nonce` included and validated per OIDC Core §3.1.2.1; Spring Security handles automatically (→ C-6) |
| CL-2 | `oidcManaged` flag for existing accounts linked via email-match | **A** — flag stays `false`; password login preserved; Google added as additional option (→ FR-9) |
| CL-3 | JWT expiry / re-auth behaviour for OIDC users | **A** — transparent re-auth: automatic redirect back to Google OIDC flow; SSO if Google session still active (→ FR-10) |
| CL-4 | Security audit logging for OIDC events | **A** — successful/failed logins, account linking, auto-provisioning logged with timestamp, provider, anonymised user ref; no plain-text PII in logs (→ FR-15) |
| CL-5 | GDPR deletion of `OidcProviderLink` | **A** — `ON DELETE CASCADE` on `userId` FK; link deleted automatically when user account is removed; `providerSubject` classified as personal data (→ Key Entities) |

---

## Notes

- All checklist items pass as of 2026-03-26.
- 5 clarifications resolved on 2026-03-26 (CL-1 to CL-5) — all encoded back into spec.md.
- Open Questions OQ-2 and OQ-3 do not block planning but must be resolved before implementation starts.
- Spec is ready for `/speckit.plan`.
