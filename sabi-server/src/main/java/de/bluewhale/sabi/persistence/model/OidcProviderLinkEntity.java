/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * JPA Entity linking a Sabi user account to an OIDC provider identity (e.g. Google).
 * GDPR: provider_subject is personal data — removed automatically via CASCADE when the user is deleted.
 *
 * @author Stefan Schubert
 */
@Table(name = "oidc_provider_link", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"user"}, callSuper = false)
public class OidcProviderLinkEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    /**
     * Owner-side of the relationship: FK → users.id (ON DELETE CASCADE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * OIDC provider name, e.g. "GOOGLE", "APPLE", "MICROSOFT".
     */
    @Basic
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /**
     * Immutable {@code sub} claim from the ID token. Survives email changes on the provider side.
     * Max 255 chars (Google sub is typically ~21 digits).
     */
    @Basic
    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    /**
     * Timestamp when this link was first created.
     */
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

}

