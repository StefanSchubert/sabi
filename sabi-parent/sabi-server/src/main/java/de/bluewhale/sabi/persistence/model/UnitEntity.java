/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "unit", schema = "sabi")
@Entity
public class UnitEntity extends TracableEntity {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    private String name;

    private String description;

    @Embedded
    private EntityState entityState;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public EntityState getEntityState() {
        return this.entityState;
    }

    @Override
    public void setEntityState(EntityState entityState) {
        this.entityState = entityState;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "name", nullable = false, insertable = true, updatable = true, length = 15, precision = 0)
    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        UnitEntity that = (UnitEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.name.equals(that.name)) return false;
        if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
        return this.entityState.equals(that.entityState);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.name.hashCode();
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + this.entityState.hashCode();
        return result;
    }
}
