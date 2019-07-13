/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "coral_catalogue", schema = "sabi")
@Entity
public class CoralCatalogueEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String scientificName;

    private String description;


// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 400, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "scientific_name", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        CoralCatalogueEntity that = (CoralCatalogueEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.scientificName.equals(that.scientificName)) return false;
        return this.description != null ? this.description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.scientificName.hashCode();
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        return result;
    }
}
