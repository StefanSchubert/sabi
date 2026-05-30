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
 * Invertebrate catalogue entry with UGC workflow support (PENDING / PUBLIC / REJECTED).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Table(name = "invertebrate_catalogue", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"i18nEntries", "proposer"})
@ToString(exclude = {"i18nEntries", "proposer"})
public class InvertebrateCatalogueEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "scientific_name", nullable = false, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "taxonomic_category", nullable = true, length = 30)
    @Basic
    private String taxonomicCategory;

    @Column(name = "care_level", nullable = true, length = 12)
    @Basic
    private String careLevel;

    @Column(name = "status", nullable = false, length = 10)
    @Basic
    private String status = "PENDING";

    @Column(name = "proposer_user_id", nullable = true)
    @Basic
    private Long proposerUserId;

    @Column(name = "proposal_date", nullable = true)
    @Basic
    private LocalDate proposalDate;

    @OneToMany(mappedBy = "catalogue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvertebrateCatalogueI18nEntity> i18nEntries = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_user_id", insertable = false, updatable = false)
    private UserEntity proposer;
}
