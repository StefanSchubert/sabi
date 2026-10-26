# Data Model: OpenID Connect Login via Google (sabi-150)

**Phase**: 1 — Design  
**Date**: 2026-03-26  
**Status**: Complete

---

## 1. Overview of Changes

| Entity / Table | Change Type | Description |
|----------------|-------------|-------------|
| `oidc_provider_link` | **NEW** | Links a Sabi user account to a Google (or future Apple/Microsoft) identity |
| `users` | **MODIFIED** | New column `oidc_managed` — distinguishes auto-provisioned OIDC accounts |

---

## 2. New Table: `oidc_provider_link`

### DDL

```sql
CREATE TABLE `oidc_provider_link`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT(20) NOT NULL,
    `provider`         VARCHAR(20) NOT NULL COMMENT 'GOOGLE | APPLE | MICROSOFT',
    `provider_subject` VARCHAR(255) NOT NULL COMMENT 'Immutable sub claim from ID token',
    `linked_at`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_on`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `optlock`          INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_provider_subject` (`provider`, `provider_subject`),
    UNIQUE KEY `uq_user_provider`    (`user_id`, `provider`),
    CONSTRAINT `fk_oidc_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'GDPR: provider_subject is personal data; deleted via CASCADE on user removal';
```

### Column Notes

| Column | Notes |
|--------|-------|
| `provider_subject` | Google's immutable `sub` claim. Survives email changes on Google side. Max 255 chars (Google sub is typically ~21 digits). |
| `provider` | Enum-like VARCHAR — constrained by application layer to `GOOGLE`, `APPLE`, `MICROSOFT`. |
| `uq_provider_subject` | Prevents the same Google account from being linked to two different Sabi users. |
| `uq_user_provider` | One link per provider per user (a user can have Google + Apple, but not two Google links). |
| `ON DELETE CASCADE` | GDPR compliance (CL-5): removing the `users` row automatically removes all linked identities. |

---

## 3. Modified Table: `users`

### DDL

```sql
ALTER TABLE `users`
    ADD COLUMN `oidc_managed` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1 if account was auto-provisioned via OIDC and has no local password';
```

### Semantics

| `oidc_managed` value | Meaning |
|----------------------|---------|
| `0` (false) | Standard account: has a local password; Google may be linked as additional option |
| `1` (true) | Auto-provisioned via OIDC: no local password set; `password` column is NULL or empty string |

---

## 4. Flyway Migration Files

### Location
```
sabi-database/src/main/resources/db/migration/version1_4_0/
├── V1_4_0_1__addOidcProviderLinkTable.sql
└── V1_4_0_2__addOidcManagedFlagToUsers.sql
```

### V1_4_0_1__addOidcProviderLinkTable.sql
Full DDL from section 2 above.

### V1_4_0_2__addOidcManagedFlagToUsers.sql
Full DDL from section 3 above.

---

## 5. JPA Entity: `OidcProviderLinkEntity`

```java
package de.bluewhale.sabi.persistence.model;

// @Table(name = "oidc_provider_link", schema = "sabi")
// @Entity, @Data, @EqualsAndHashCode(callSuper = false)
// Fields:
//   Long id (PK, auto-generated)
//   UserEntity user (ManyToOne, FK user_id, NOT NULL)
//   String provider  ("GOOGLE" | "APPLE" | "MICROSOFT")
//   String providerSubject  (the immutable `sub` claim)
//   LocalDateTime linkedAt
```

### Relationship to `UserEntity`

```
UserEntity  1 ────── * OidcProviderLinkEntity
            (user_id FK, ON DELETE CASCADE)
```

`UserEntity` gains a `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)` collection (or navigated only from `OidcProviderLinkRepository` — preferred for lazy loading).

---

## 6. Modified Entity: `UserEntity`

New field added:

```java
@Basic
@Column(name = "oidc_managed", nullable = false)
private boolean oidcManaged = false;
```

---

## 7. Repository: `OidcProviderLinkRepository`

Spring Data JPA interface:

```java
// OidcProviderLinkRepository extends JpaRepository<OidcProviderLinkEntity, Long>
// Custom finders:
//   Optional<OidcProviderLinkEntity> findByProviderAndProviderSubject(String provider, String providerSubject)
//   Optional<OidcProviderLinkEntity> findByUserAndProvider(UserEntity user, String provider)
```

---

## 8. Entity Relationship Diagram (ERD — text)

```
users (existing)
  id PK
  email UNIQUE
  username
  password
  oidc_managed  ← NEW
  ...

oidc_provider_link (NEW)
  id PK
  user_id FK → users.id  ON DELETE CASCADE
  provider  ("GOOGLE" | "APPLE" | "MICROSOFT")
  provider_subject  (Google sub — immutable)
  linked_at
  UNIQUE (provider, provider_subject)
  UNIQUE (user_id, provider)
```

---

*Phase 1 data model complete. See contracts/oidc-api.md for the REST interface.*

