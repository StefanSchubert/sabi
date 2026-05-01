/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Localized names and descriptions for fish roles.
 */
@Table(name = "localized_fish_role", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class LocalizedFishRoleEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = false, updatable = false)
    @Basic
    private Long id;

    @Column(name = "role_id", nullable = false)
    @Basic
    private Integer roleId;

    @Column(name = "language_code", nullable = false, length = 2)
    @Basic
    private String languageCode;

    @Column(name = "name", nullable = false, length = 80)
    @Basic
    private String name;

    @Column(name = "description", nullable = true, length = 512)
    @Basic
    private String description;

}
