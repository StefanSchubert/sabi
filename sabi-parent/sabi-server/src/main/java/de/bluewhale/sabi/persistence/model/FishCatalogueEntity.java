package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@Table(name = "fish_catalogue", schema = "", catalog = "sabi")
@Entity
public class FishCatalogueEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long id;

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private String scientificName;

    @javax.persistence.Column(name = "scientific_name", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    private String description;

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 400, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String meerwasserwikiUrl;

    @javax.persistence.Column(name = "meerwasserwiki_url", nullable = true, insertable = true, updatable = true, length = 120, precision = 0)
    @Basic
    public String getMeerwasserwikiUrl() {
        return meerwasserwikiUrl;
    }

    public void setMeerwasserwikiUrl(String meerwasserwikiUrl) {
        this.meerwasserwikiUrl = meerwasserwikiUrl;
    }

    private Timestamp createdOn;

    @javax.persistence.Column(name = "created_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    private Timestamp lastmodOn;

    @javax.persistence.Column(name = "lastmod_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getLastmodOn() {
        return lastmodOn;
    }

    public void setLastmodOn(Timestamp lastmodOn) {
        this.lastmodOn = lastmodOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FishCatalogueEntity that = (FishCatalogueEntity) o;

        if (id != that.id) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (lastmodOn != null ? !lastmodOn.equals(that.lastmodOn) : that.lastmodOn != null) return false;
        if (meerwasserwikiUrl != null ? !meerwasserwikiUrl.equals(that.meerwasserwikiUrl) : that.meerwasserwikiUrl != null)
            return false;
        if (scientificName != null ? !scientificName.equals(that.scientificName) : that.scientificName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (scientificName != null ? scientificName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (meerwasserwikiUrl != null ? meerwasserwikiUrl.hashCode() : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        result = 31 * result + (lastmodOn != null ? lastmodOn.hashCode() : 0);
        return result;
    }
}
