/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended fish catalogue entry with UGC workflow support.
 * Replaces the deprecated {@link FishCatalogueEntity} (002-fish-stock-catalogue).
 *
 * @author Stefan Schubert
 */
@Table(name = "fish_catalogue", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"i18nEntries", "proposer"})
@ToString(exclude = {"i18nEntries", "proposer"})
public class FishCatalogueEntryEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "scientific_name", nullable = true, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "status", nullable = false, length = 10)
    @Basic
    private String status = "PUBLIC";

    @Column(name = "proposer_user_id", nullable = true)
    @Basic
    private Long proposerUserId;

    @Column(name = "proposal_date", nullable = true)
    @Basic
    private LocalDate proposalDate;

    /**
     * @deprecated Use i18nEntries for localized descriptions. Will be removed in v1.6.0.
     */
    @Deprecated
    @Column(name = "description", nullable = true, length = 400)
    @Basic
    private String description;

    /**
     * @deprecated Use i18nEntries for localized reference URLs. Will be removed in v1.6.0.
     */
    @Deprecated
    @Column(name = "meerwasserwiki_url", nullable = true, length = 120)
    @Basic
    private String meerwasserwikiUrl;

    @OneToMany(mappedBy = "catalogueEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FishCatalogueI18nEntity> i18nEntries = new ArrayList<>();

    /**
     * Only for Admin-Queries. insertable=false, updatable=false because proposerUserId is the scalar FK.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_user_id", insertable = false, updatable = false)
    private UserEntity proposer;

}

