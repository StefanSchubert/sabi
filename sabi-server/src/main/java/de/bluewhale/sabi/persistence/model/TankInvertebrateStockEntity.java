/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invertebrate entry in a user's marine tank stock.
 * Soft-delete via deleted_at (set on aquarium cascade deletion).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@NamedQueries({
        @NamedQuery(name = "TankInvertebrateStock.getByAquariumAndUser",
                query = "SELECT i FROM TankInvertebrateStockEntity i " +
                        "WHERE i.aquariumId = :pAquariumId " +
                        "AND i.user.id = :pUserId " +
                        "AND i.deletedAt IS NULL")
})
@Table(name = "invertebrate_stock", schema = "sabi")
@Entity
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(exclude = {"user", "catalogueEntry", "waterSensitivities"}, callSuper = false)
@ToString(exclude = {"user", "catalogueEntry", "waterSensitivities"})
public class TankInvertebrateStockEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "invertebrate_catalogue_id", nullable = true)
    @Basic
    private Long invertebrateCatalogueId;

    @Column(name = "species_name", nullable = false, length = 255)
    @Basic
    private String speciesName;

    @Column(name = "scientific_name", nullable = true, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "taxonomic_category", nullable = false, length = 12)
    @Basic
    private String taxonomicCategory;

    @Column(name = "care_level", nullable = true, length = 12)
    @Basic
    private String careLevel;

    @Column(name = "mobility", nullable = true, length = 10)
    @Basic
    private String mobility;

    @Column(name = "ecological_role", nullable = true, length = 15)
    @Basic
    private String ecologicalRole;

    @Column(name = "activity_pattern", nullable = true, length = 10)
    @Basic
    private String activityPattern;

    @Column(name = "external_ref_url", nullable = true, length = 512)
    @Basic
    private String externalRefUrl;

    @Column(name = "notes", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String notes;

    @Column(name = "added_on", nullable = false)
    @Basic
    private LocalDate addedOn;

    @Column(name = "departed_on", nullable = true)
    @Basic
    private LocalDate departedOn;

    @Column(name = "departure_reason", nullable = true, length = 30)
    @Basic
    private String departureReason;

    @Column(name = "departure_note", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String departureNote;

    @Column(name = "deleted_at", nullable = true)
    @Basic
    private LocalDateTime deletedAt;

    /**
     * Owner-side of the relationship for ownership checks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Optional link to the invertebrate catalogue entry.
     * insertable=false, updatable=false because invertebrateCatalogueId is the scalar FK.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invertebrate_catalogue_id", insertable = false, updatable = false)
    private InvertebrateCatalogueEntity catalogueEntry;

    /**
     * Water sensitivity units — managed via service layer (delete-all + re-insert).
     */
    @OneToMany(mappedBy = "invertebrateStock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvertebrateWaterSensitivityEntity> waterSensitivities = new ArrayList<>();
}
