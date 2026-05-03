# Specification Quality Checklist: Fish Stock Management & Fish Catalogue

**Purpose**: Validate specification completeness and quality before proceeding to planning  
**Created**: 2026-04-05  
**Feature**: [spec.md](../spec.md)

---

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- All 25 Functional Requirements are covered across 6 independently-testable user stories (P1–P6).
- Scope section explicitly calls out the sabi-10 copyright decision (no wiki import) and defers coral/invertebrate stock to a future feature.
- i18n completeness requirement (FR-022) and WCAG 2.1 AA compliance are explicitly mandated.
- Flyway migration need is identified under NFR Wartbarkeit and in Assumptions.
- Photo storage approach (filesystem volume, not BLOB) is documented as Constraint C-4 and in Assumptions.
- **Clarification loop completed (2026-04-05): 5 questions asked and answered.**
  - Q1: Unique-Constraint for scientific name applies only to Pending + Public (not Rejected) → reflected in FR-012, FR-015, Assumptions.
  - Q2: Scientific name is copied (snapshot) into `TankFishStock` at link time; catalogue changes do NOT propagate → reflected in FR-009, User Story 3 AC-5, Assumptions.
  - Q3: "Remove" has dual semantics — Departure-Record for genuine departures (retained); physical delete only if no Departure-Record exists → reflected in FR-024, Edge Cases, Scope, Assumptions.
  - Q4: Creator may edit scientific name of a Pending entry; non-blocking duplicate warning re-evaluated on each change → reflected in FR-019, FR-015.
  - Q5: No standalone photo direct-link endpoint; photo bytes served exclusively via the fish-entry API endpoint (inherits full Auth + Ownership checks) → reflected in FR-025, C-7, Edge Cases.
- Spec status advanced from **Draft** → **Clarified**.

**Ready for `/speckit.plan`** — all clarifications resolved.

