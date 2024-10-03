/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Table which contains known plague status values.
 */
@Table(name = "plague_status", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "localizedPlagueStatusEntities",callSuper = false)
public class PlagueStatusEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    // Unidirectional for now - as this contains more static data, we we won't provide a admin gui for it.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="plague_status_id")
    private List<LocalizedPlagueStatusEntity> localizedPlagueStatusEntities = new ArrayList<LocalizedPlagueStatusEntity>();

}
