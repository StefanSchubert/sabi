/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.Basic;
import java.sql.Timestamp;

/**
 *
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
public abstract class TracableEntity {

    Timestamp createdOn;

    @javax.persistence.Column(name = "created_on", nullable = false, insertable = true, updatable = false, length = 19, precision = 0)
    @Basic
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }


    Timestamp lastmodOn;

    @javax.persistence.Column(name = "lastmod_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getLastmodOn() {
        return lastmodOn;
    }

    public void setLastmodOn(Timestamp lastmodOn) {
        this.lastmodOn = lastmodOn;
    }

}
