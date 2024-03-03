/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


@NamedQueries({@NamedQuery(name = "PlagueRecord.getPlagueRecord",
        query = "select a from PlagueRecordEntity a, AquariumEntity t where a.id = :pPlagueRecordId " +
                "and a.aquarium.id = :pTankID " +
                "and a.aquarium.id = t.id " +
                "and t.user.id = :pUserID"),
        @NamedQuery(name = "PlagueRecord.getAllPlagueRecordsForTank",
                query = "select a from PlagueRecordEntity a where a.aquarium.id = :pTankID"),
        @NamedQuery(name = "PlagueRecord.getUsersPlagueRecords",
                query = "select a FROM PlagueRecordEntity a, AquariumEntity t " +
                        "where a.aquarium.id = t.id " +
                        "and t.user.id = :pUserID")})
@Table(name = "plague_record", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"user", "aquarium"}, callSuper = false)
public class PlagueRecordEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @Column(name = "observed_on", nullable = false, insertable = true, updatable = true)
    @Basic
    private LocalDateTime observedOn;

    @Column(name = "plague_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Integer plagueId;

    @Column(name = "observed_plague_status", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Integer observedPlagueStatus;

    /**
     * The plague intervall id groups all records together, that belong to the same
     * plague occurrence. This eases data queries and investigations.
     * The number is unique only in combination with the tuple (tankID,plageID)
     * and is provided programmatically.
     */
    @Column(name = "plague_intervall_id", nullable = false, insertable = true, updatable = true)
    @Basic
    private Integer plagueIntervallId;


    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private AquariumEntity aquarium;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}
