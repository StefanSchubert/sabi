/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Master table for fish role classifications.
 * Each role has an enum_key for programmatic access and localized names/descriptions.
 */
@Table(name = "fish_role", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class FishRoleEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Integer id;

    @Column(name = "enum_key", nullable = false, length = 40)
    @Basic
    private String enumKey;

}
