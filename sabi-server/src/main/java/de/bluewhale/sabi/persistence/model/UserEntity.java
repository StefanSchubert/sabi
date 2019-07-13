/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Stefan
 * Date: 12.03.15
 */
@Table(name = "users", schema = "sabi")
@Entity
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
    private List<TreatmentEntity> treatments = new ArrayList<TreatmentEntity>();


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


// --------------------- GETTER / SETTER METHODS ---------------------


    public List<CoralEntity> getCorals() {
        return this.corals;
    }

    public void setCorals(List<CoralEntity> corals) {
        this.corals = corals;
    }

    public List<FishEntity> getFishes() {
        return this.fishes;
    }

    public void setFishes(List<FishEntity> fishes) {
        this.fishes = fishes;
    }

    public List<MeasurementEntity> getMeasurements() {
        return this.measurements;
    }

    public void setMeasurements(List<MeasurementEntity> measurements) {
        this.measurements = measurements;
    }

    public List<TreatmentEntity> getTreatments() {
        return this.treatments;
    }

    public void setTreatments(List<TreatmentEntity> treatments) {
        this.treatments = treatments;
    }

    public List<AquariumEntity> getAquariums() {
        return this.aquariums;
    }

    public void setAquariums(List<AquariumEntity> pAquariums) {
        this.aquariums = pAquariums;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getValidateToken() {
        return validateToken;
    }

    public void setValidateToken(String validateToken) {
        this.validateToken = validateToken;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        UserEntity that = (UserEntity) o;

        if (this.validated != that.validated) return false;
        if (!this.id.equals(that.id)) return false;
        if (this.aquariums != null ? !this.aquariums.equals(that.aquariums) : that.aquariums != null) return false;
        if (this.corals != null ? !this.corals.equals(that.corals) : that.corals != null) return false;
        if (this.fishes != null ? !this.fishes.equals(that.fishes) : that.fishes != null) return false;
        if (this.measurements != null ? !this.measurements.equals(that.measurements) : that.measurements != null) return false;
        if (this.treatments != null ? !this.treatments.equals(that.treatments) : that.treatments != null) return false;
        if (!this.email.equals(that.email)) return false;
        if (!this.username.equals(that.username)) return false;
        if (!this.password.equals(that.password)) return false;
        if (this.validateToken != null ? !this.validateToken.equals(that.validateToken) : that.validateToken != null)
            return false;
        if (this.language != null ? !this.language.equals(that.language) : that.language != null) return false;
        return this.country != null ? this.country.equals(that.country) : that.country == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + (this.aquariums != null ? this.aquariums.hashCode() : 0);
        result = 31 * result + (this.corals != null ? this.corals.hashCode() : 0);
        result = 31 * result + (this.fishes != null ? this.fishes.hashCode() : 0);
        result = 31 * result + (this.measurements != null ? this.measurements.hashCode() : 0);
        result = 31 * result + (this.treatments != null ? this.treatments.hashCode() : 0);
        result = 31 * result + this.email.hashCode();
        result = 31 * result + this.username.hashCode();
        result = 31 * result + this.password.hashCode();
        result = 31 * result + (this.validateToken != null ? this.validateToken.hashCode() : 0);
        result = 31 * result + (this.validated ? 1 : 0);
        result = 31 * result + (this.language != null ? this.language.hashCode() : 0);
        result = 31 * result + (this.country != null ? this.country.hashCode() : 0);
        return result;
    }
}
