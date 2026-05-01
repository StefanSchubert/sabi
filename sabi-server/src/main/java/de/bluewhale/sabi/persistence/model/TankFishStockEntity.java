/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a fish entry in a user's tank stock.
 * Renamed from FishEntity to TankFishStockEntity (002-fish-stock-catalogue).
 * The underlying table remains 'fish' for backward compatibility.
 *
 * @author Stefan Schubert
 */
@NamedQueries({
        @NamedQuery(name = "TankFishStock.getUsersFish",
                query = "SELECT f FROM TankFishStockEntity f " +
                        "WHERE f.id = :pFishId " +
                        "AND :pUserId IN (SELECT a.user.id FROM AquariumEntity a WHERE a.id = f.aquariumId)")
})
@Table(name = "fish", schema = "sabi")
@Entity
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(exclude = "user", callSuper = false)
public class TankFishStockEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "fish_catalogue_id", nullable = true)
    @Basic
    private Long fishCatalogueId;

    @Column(name = "common_name", nullable = false, length = 255)
    @Basic
    private String commonName;

    @Column(name = "scientific_name", nullable = true, length = 255)
    @Basic
    private String scientificName;

    @Column(name = "nickname", nullable = true, length = 60)
    @Basic
    private String nickname;

    @Column(name = "external_ref_url", nullable = true, length = 512)
    @Basic
    private String externalRefUrl;

    @Column(name = "added_on", nullable = false)
    @Basic
    private LocalDate addedOn;

    @Column(name = "exodus_on", nullable = true)
    @Basic
    private LocalDate exodusOn;

    @Column(name = "departure_reason", nullable = true, length = 30)
    @Basic
    private String departureReason;

    @Column(name = "departure_note", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String departureNote;

    @Column(name = "observed_behavior", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String observedBehavior;

    @Column(name = "deleted_at", nullable = true)
    @Basic
    private LocalDateTime deletedAt;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}

