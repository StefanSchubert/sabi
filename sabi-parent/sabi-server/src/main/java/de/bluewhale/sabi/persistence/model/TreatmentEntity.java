/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "treatment", schema = "sabi")
@Entity
public class TreatmentEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long aquariumId;

    private Timestamp givenOn;

    private float amount;

    private Integer unitId;

    private Long remedyId;

    private String description;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;


// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "amount", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @javax.persistence.Column(name = "given_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getGivenOn() {
        return givenOn;
    }

    public void setGivenOn(Timestamp givenOn) {
        this.givenOn = givenOn;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "remedy_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getRemedyId() {
        return remedyId;
    }

    public void setRemedyId(Long remedyId) {
        this.remedyId = remedyId;
    }

    @javax.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        TreatmentEntity that = (TreatmentEntity) o;

        if (Float.compare(that.amount, this.amount) != 0) return false;
        if (!this.id.equals(that.id)) return false;
        if (!this.aquariumId.equals(that.aquariumId)) return false;
        if (!this.givenOn.equals(that.givenOn)) return false;
        if (!this.unitId.equals(that.unitId)) return false;
        if (!this.remedyId.equals(that.remedyId)) return false;
        if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
        return this.user.equals(that.user);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.aquariumId.hashCode();
        result = 31 * result + this.givenOn.hashCode();
        result = 31 * result + (this.amount != +0.0f ? Float.floatToIntBits(this.amount) : 0);
        result = 31 * result + this.unitId.hashCode();
        result = 31 * result + this.remedyId.hashCode();
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + this.user.hashCode();
        return result;
    }
}
