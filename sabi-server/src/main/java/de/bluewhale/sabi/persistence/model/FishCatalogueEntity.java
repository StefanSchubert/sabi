/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "fish_catalogue", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public class FishCatalogueEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @jakarta.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @jakarta.persistence.Column(name = "scientific_name", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    private String scientificName;

    @jakarta.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 400, precision = 0)
    @Basic
    private String description;

    @jakarta.persistence.Column(name = "meerwasserwiki_url", nullable = true, insertable = true, updatable = true, length = 120, precision = 0)
    @Basic
    private String meerwasserwikiUrl;

}
