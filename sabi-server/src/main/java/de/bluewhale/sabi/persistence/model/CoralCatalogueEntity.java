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
 * Coral catalogue entry with UGC workflow support (PENDING / PUBLIC / REJECTED).
 * The virtual column active_scientific_name is NOT mapped — it is a DB-side virtual column
 * used only for the partial-unique index; JPA must not manage it.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Table(name = "coral_catalogue", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"i18nEntries", "proposer"})
@ToString(exclude = {"i18nEntries", "proposer"})
public class CoralCatalogueEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "scientific_name", nullable = false, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "classification", nullable = false, length = 5)
    @Basic
    private String classification;

    @Column(name = "care_level", nullable = false, length = 12)
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
    private List<CoralCatalogueI18nEntity> i18nEntries = new ArrayList<>();

    /** Only for Admin-Queries. insertable=false, updatable=false because proposerUserId is the scalar FK. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_user_id", insertable = false, updatable = false)
    private UserEntity proposer;
}
