/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Author: Stefan
 * Date: 12.03.15
 */
@Table(name = "users", schema = "sabi")
@Entity
public class UserEntity extends TracableEntity {
// ------------------------------ FIELDS ------------------------------

    // TODO StS 22.09.15: use UUID
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private List<AquariumEntity> aquariums = new ArrayList<>();

    private String email;

    private String password;

    private String validateToken;

    private boolean validated;

// --------------------- GETTER / SETTER METHODS ---------------------

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    public List<AquariumEntity> getAquariums() {
        return this.aquariums;
    }

    public void setAquariums(List<AquariumEntity> pAquariums) {
        this.aquariums = pAquariums;
    }

    @javax.persistence.Column(name = "email", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "password", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @javax.persistence.Column(name = "validate_token", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getValidateToken() {
        return validateToken;
    }

    public void setValidateToken(String validateToken) {
        this.validateToken = validateToken;
    }

    @javax.persistence.Column(name = "validated", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    @Basic
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
        if (o == null || getClass() != o.getClass()) return false;

        UserEntity that = (UserEntity) o;

        if (id != that.id) return false;
        if (validated != that.validated) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (lastmodOn != null ? !lastmodOn.equals(that.lastmodOn) : that.lastmodOn != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (validateToken != null ? !validateToken.equals(that.validateToken) : that.validateToken != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (validateToken != null ? validateToken.hashCode() : 0);
        result = 31 * result + (validated ? 1 : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        result = 31 * result + (lastmodOn != null ? lastmodOn.hashCode() : 0);
        return result;
    }
}
