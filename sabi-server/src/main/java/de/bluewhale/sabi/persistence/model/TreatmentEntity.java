/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Table(name = "treatment", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "user")
public class TreatmentEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @jakarta.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @jakarta.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long aquariumId;

    @jakarta.persistence.Column(name = "given_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    private Timestamp givenOn;

    @jakarta.persistence.Column(name = "amount", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private float amount;

    @jakarta.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer unitId;

    @jakarta.persistence.Column(name = "remedy_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long remedyId;

    @jakarta.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    private String description;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}
