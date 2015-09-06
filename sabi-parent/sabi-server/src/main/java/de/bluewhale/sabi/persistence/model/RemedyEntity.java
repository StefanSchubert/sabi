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
@Table(name = "remedy", schema = "", catalog = "sabi")
@Entity
public class RemedyEntity {

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

    private String productname;

    @javax.persistence.Column(name = "productname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getProductname() {
        return productname;
    }

    public void setProductname(String productname) {
        this.productname = productname;
    }

    private String vendor;

    @javax.persistence.Column(name = "vendor", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemedyEntity that = (RemedyEntity) o;

        if (id != that.id) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (productname != null ? !productname.equals(that.productname) : that.productname != null) return false;
        if (vendor != null ? !vendor.equals(that.vendor) : that.vendor != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (productname != null ? productname.hashCode() : 0);
        result = 31 * result + (vendor != null ? vendor.hashCode() : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        return result;
    }
}
