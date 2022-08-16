/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.SizeUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NamedQueries({@NamedQuery(name = "Aquarium.getAquarium",
        query = "select a from AquariumEntity a where a.id = :pTankId and a.user.id = :pUserID"),
        @NamedQuery(name = "Aquarium.getUsersAquariums",
                query = "select a FROM AquariumEntity a where a.user.id = :pUserID")})
@Table(name = "aquarium", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude={"user", "measurements"})
public class AquariumEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @Basic
    @Column(name = "size", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    private Integer size;

    @Column(name = "size_unit", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Enumerated(EnumType.STRING)
    private SizeUnit sizeUnit;

    @Basic
    @Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String description;

    @Basic
    @Column(name = "temperature_api_key", nullable = true, insertable = true, updatable = true, length = 48)
    private String temperatureApiKey;

    @Column(name = "active", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    private Boolean active;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aquarium", cascade = CascadeType.ALL)
    private List<MeasurementEntity> measurements = new ArrayList<MeasurementEntity>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aquarium", cascade = CascadeType.ALL)
    private List<PlagueRecordEntity> plagueRecords = new ArrayList<PlagueRecordEntity>();

    @Temporal(TemporalType.DATE)
    @Column(name = "inception_date", nullable = true, insertable = true, updatable = true)
    private Date inceptionDate;

}
