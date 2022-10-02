/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Table which contains trackable plagues of our tanks.
 */
@Table(name = "plague", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "localizedPlagueEntities")
public class PlagueEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @Column(name = "scientific_name", nullable = false, insertable = true, updatable = true, length = 80)
    @Basic
    private String scientificName;

    // Unidirectional for now - as this contains more static data, we we won't provide a admin gui for it.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="plague_id")
    private List<LocalizedPlagueEntity> localizedPlagueEntities = new ArrayList<LocalizedPlagueEntity>();

}
