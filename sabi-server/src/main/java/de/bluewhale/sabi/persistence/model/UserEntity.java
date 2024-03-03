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
 * Author: Stefan
 * Date: 12.03.15
 */
@Table(name = "users", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"aquariums","corals","fishes","measurements","treatments"},callSuper = false)
public class UserEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    // TODO StS 22.09.15: use UUID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<AquariumEntity> aquariums = new ArrayList<AquariumEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<CoralEntity> corals = new ArrayList<CoralEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<FishEntity> fishes = new ArrayList<FishEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<MeasurementEntity> measurements = new ArrayList<MeasurementEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<PlagueRecordEntity> plagueRecords = new ArrayList<PlagueRecordEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<TreatmentEntity> treatments = new ArrayList<TreatmentEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<UserMeasurementReminderEntity> userMeasurementReminders = new ArrayList<UserMeasurementReminderEntity>();

    @Basic
    @Column(name = "email", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String email;

    @Basic
    @Column(name = "username", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String username;

    @Basic
    @Column(name = "password", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String password;

    // This Token is used to validate the user registration (i.e. if the email is valid and belongs to the user.)
    @Basic
    @Column(name = "validate_token", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String validateToken;

    @Basic
    @Column(name = "validated", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    private boolean validated;

    @Basic
    @Column(name = "language", nullable = false, insertable = true, updatable = true, length = 2, precision = 0)
    private String language;

    @Basic
    @Column(name = "country", nullable = false, insertable = true, updatable = true, length = 2, precision = 0)
    private String country;
    
}
