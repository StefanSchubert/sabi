/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Timestamp;

@Table(name = "treatment", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "user")
public class TreatmentEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long aquariumId;

    @javax.persistence.Column(name = "given_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    private Timestamp givenOn;

    @javax.persistence.Column(name = "amount", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private float amount;

    @javax.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer unitId;

    @javax.persistence.Column(name = "remedy_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long remedyId;

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    private String description;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}
