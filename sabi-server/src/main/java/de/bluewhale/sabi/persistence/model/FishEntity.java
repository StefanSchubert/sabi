/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@NamedQueries({@NamedQuery(name="Fish.getUsersFish",
        query="select f from FishEntity f where :pUserId in (select a.user.id from AquariumEntity a where a.id = f.aquariumId) and f.id = :pFishId")})
@Table(name = "fish", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "user",callSuper = false)
public class FishEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @jakarta.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @jakarta.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long aquariumId;

    @jakarta.persistence.Column(name = "fish_catalogue_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long fishCatalogueId;

    @jakarta.persistence.Column(name = "added_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    private Timestamp addedOn;

    @jakarta.persistence.Column(name = "exodus_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    private Timestamp exodusOn;

    @jakarta.persistence.Column(name = "nickname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    private String nickname;

    @jakarta.persistence.Column(name = "observed_behavior", nullable = true, insertable = true, updatable = true, length = 65535, precision = 0)
    @Basic
    private String observedBehavior;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}
