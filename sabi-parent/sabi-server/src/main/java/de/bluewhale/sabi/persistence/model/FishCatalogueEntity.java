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
@Table(name = "fish_catalogue", schema = "sabi")
@Entity
public class FishCatalogueEntity extends TracableEntity {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String scientificName;

    private String description;

    private String meerwasserwikiUrl;

    @Embedded
    private EntityState entityState;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 400, precision = 0)
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

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "meerwasserwiki_url", nullable = true, insertable = true, updatable = true, length = 120, precision = 0)
    @Basic
    public String getMeerwasserwikiUrl() {
        return meerwasserwikiUrl;
    }

    public void setMeerwasserwikiUrl(String meerwasserwikiUrl) {
        this.meerwasserwikiUrl = meerwasserwikiUrl;
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

        FishCatalogueEntity that = (FishCatalogueEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.scientificName.equals(that.scientificName)) return false;
        if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
        if (this.meerwasserwikiUrl != null ? !this.meerwasserwikiUrl.equals(that.meerwasserwikiUrl) : that.meerwasserwikiUrl != null)
            return false;
        return this.entityState.equals(that.entityState);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.scientificName.hashCode();
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + (this.meerwasserwikiUrl != null ? this.meerwasserwikiUrl.hashCode() : 0);
        result = 31 * result + this.entityState.hashCode();
        return result;
    }
}
