package de.bluewhale.sabi.persistence;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@Table(name = "aquarium", schema = "", catalog = "sabi")
@Entity
public class AquariumEntity {

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

    private Integer size;

    @javax.persistence.Column(name = "size", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    private String sizeUnit;

    @javax.persistence.Column(name = "size_unit", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public String getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(String sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    private String validateToken;

    @javax.persistence.Column(name = "validate_token", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getValidateToken() {
        return validateToken;
    }

    public void setValidateToken(String validateToken) {
        this.validateToken = validateToken;
    }

    private Boolean active;

    @javax.persistence.Column(name = "active", nullable = true, insertable = true, updatable = true, length = 3, precision = 0)
    @Basic
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    private Long userId;

    @javax.persistence.Column(name = "user_id", nullable = true, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

        AquariumEntity that = (AquariumEntity) o;

        if (id != that.id) return false;
        if (active != null ? !active.equals(that.active) : that.active != null) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (lastmodOn != null ? !lastmodOn.equals(that.lastmodOn) : that.lastmodOn != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (sizeUnit != null ? !sizeUnit.equals(that.sizeUnit) : that.sizeUnit != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (validateToken != null ? !validateToken.equals(that.validateToken) : that.validateToken != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (sizeUnit != null ? sizeUnit.hashCode() : 0);
        result = 31 * result + (validateToken != null ? validateToken.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        result = 31 * result + (lastmodOn != null ? lastmodOn.hashCode() : 0);
        return result;
    }
}
