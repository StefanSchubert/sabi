/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Localized fields for a fish catalogue entry.
 * New entity introduced in 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Table(name = "fish_catalogue_i18n", schema = "sabi",
        uniqueConstraints = @UniqueConstraint(columnNames = {"catalogue_id", "language_code"}))
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"catalogueEntry"})
@ToString(exclude = {"catalogueEntry"})
public class FishCatalogueI18nEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "catalogue_id", nullable = false, insertable = true, updatable = false)
    @Basic
    private Long catalogueId;

    @Column(name = "language_code", nullable = false, length = 2)
    @Basic
    private String languageCode;

    @Column(name = "common_name", nullable = true, length = 255)
    @Basic
    private String commonName;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String description;

    @Column(name = "reference_url", nullable = true, length = 512)
    @Basic
    private String referenceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogue_id", insertable = false, updatable = false)
    private FishCatalogueEntryEntity catalogueEntry;

}

