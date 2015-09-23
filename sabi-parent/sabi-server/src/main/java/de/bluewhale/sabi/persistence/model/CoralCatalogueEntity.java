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
@Table(name = "coral_catalogue", schema = "", catalog = "sabi")
@Entity
public class CoralCatalogueEntity extends TracableEntity {

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoralCatalogueEntity that = (CoralCatalogueEntity) o;

        if (id != that.id) return false;
        if (created_On != null ? !created_On.equals(that.created_On) : that.created_On != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (lastmod_On != null ? !lastmod_On.equals(that.lastmod_On) : that.lastmod_On != null) return false;
        if (scientificName != null ? !scientificName.equals(that.scientificName) : that.scientificName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (scientificName != null ? scientificName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (created_On != null ? created_On.hashCode() : 0);
        result = 31 * result + (lastmod_On != null ? lastmod_On.hashCode() : 0);
        return result;
    }
}
