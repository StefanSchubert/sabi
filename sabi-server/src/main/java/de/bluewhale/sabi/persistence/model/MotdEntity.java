/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Table which contains the message of today content.
 */
@NamedQueries({@NamedQuery(name = "Motd.getValidModt",
        query = "select a from MotdEntity a where a.publishDate <= current_timestamp " +
                "and (a.vanishDate is null or a.vanishDate > current_timestamp)")})
@Table(name = "motd", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "localizedMotdEntities")
public class MotdEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    // Unidirectional for now - as this contains more static data, we we won't provide a admin gui for it.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="motd_id")
    private List<LocalizedMotdEntity> localizedMotdEntities = new ArrayList<LocalizedMotdEntity>();

    @Basic
    @Column(name = "publish_date", nullable = false)
    protected LocalDateTime publishDate;

    @Basic
    @Column(name = "vanish_date", nullable = true)
    protected LocalDateTime vanishDate;

}
