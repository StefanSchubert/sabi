/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Table(name = "user_measurement_reminder", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"user"},callSuper = false)
public class UserMeasurementReminderEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer unitId;

    @Column(name = "pastdays", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    @Basic
    private Integer pastdays;

    @Column(name = "active", nullable = false, insertable = true, updatable = true)
    @Basic
    private boolean active;
}
