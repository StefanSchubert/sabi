/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "remedy", schema = "sabi")
@Entity
public class RemedyEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String productname;

    private String vendor;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "productname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getProductname() {
        return productname;
    }

    public void setProductname(String productname) {
        this.productname = productname;
    }

    @javax.persistence.Column(name = "vendor", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        RemedyEntity that = (RemedyEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.productname.equals(that.productname)) return false;
        return this.vendor != null ? this.vendor.equals(that.vendor) : that.vendor == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.productname.hashCode();
        result = 31 * result + (this.vendor != null ? this.vendor.hashCode() : 0);
        return result;
    }
}