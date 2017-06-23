/*
 * Copyright (c) 2017 by Stefan Schubert
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
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

// --------------------- GETTER / SETTER METHODS ---------------------


    public List<AquariumEntity> getAquariums() {
        return this.aquariums;
    }

    public void setAquariums(List<AquariumEntity> pAquariums) {
        this.aquariums = pAquariums;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Deprecated // "Only require on TO but not on Entity! Will be removed on next refactoring."
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
