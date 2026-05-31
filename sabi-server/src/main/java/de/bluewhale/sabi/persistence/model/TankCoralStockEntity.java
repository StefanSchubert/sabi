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

/**
 * Represents a coral entry in a user's marine tank stock.
 * Soft-delete via deleted_at (set on aquarium cascade deletion).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@NamedQueries({
        @NamedQuery(name = "TankCoralStock.getCoralsByAquariumAndUser",
                query = "SELECT c FROM TankCoralStockEntity c " +
                        "WHERE c.aquariumId = :pAquariumId " +
                        "AND c.user.id = :pUserId " +
                        "AND c.deletedAt IS NULL")
})
@Table(name = "coral_stock", schema = "sabi")
@Entity
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(exclude = {"user", "catalogueEntry"}, callSuper = false)
@ToString(exclude = {"user", "catalogueEntry"})
public class TankCoralStockEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "coral_catalogue_id", nullable = true)
    @Basic
    private Long coralCatalogueId;

    @Column(name = "species_name", nullable = false, length = 255)
    @Basic
    private String speciesName;

    @Column(name = "scientific_name", nullable = true, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "classification", nullable = true, length = 5)
    @Basic
    private String classification;

    @Column(name = "care_level", nullable = true, length = 12)
    @Basic
    private String careLevel;

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
     * Optional link to the coral catalogue entry.
     * insertable=false, updatable=false because coralCatalogueId is the scalar FK.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coral_catalogue_id", insertable = false, updatable = false)
    private CoralCatalogueEntity catalogueEntry;
}

