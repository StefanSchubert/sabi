/*
 * Copyright (c) 2018 by Stefan Schubert
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
public class UserEntity extends TracableEntity {
// ------------------------------ FIELDS ------------------------------

    // TODO StS 22.09.15: use UUID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<AquariumEntity> aquariums = new ArrayList<AquariumEntity>();

    @Basic
    @Column(name = "email", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String email;

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

    @Embedded
    private EntityState entityState;

// --------------------- GETTER / SETTER METHODS ---------------------

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

    public EntityState getEntityState() {
        return this.entityState;
    }

    public void setEntityState(EntityState entityState) {
        this.entityState = entityState;
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
        if (!this.email.equals(that.email)) return false;
        if (this.password != null ? !this.password.equals(that.password) : that.password != null) return false;
        if (this.validateToken != null ? !this.validateToken.equals(that.validateToken) : that.validateToken != null)
            return false;
        if (this.language != null ? !this.language.equals(that.language) : that.language != null) return false;
        if (this.country != null ? !this.country.equals(that.country) : that.country != null) return false;
        return this.entityState.equals(that.entityState);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + (this.aquariums != null ? this.aquariums.hashCode() : 0);
        result = 31 * result + this.email.hashCode();
        result = 31 * result + (this.password != null ? this.password.hashCode() : 0);
        result = 31 * result + (this.validateToken != null ? this.validateToken.hashCode() : 0);
        result = 31 * result + (this.validated ? 1 : 0);
        result = 31 * result + (this.language != null ? this.language.hashCode() : 0);
        result = 31 * result + (this.country != null ? this.country.hashCode() : 0);
        result = 31 * result + this.entityState.hashCode();
        return result;
    }
}
