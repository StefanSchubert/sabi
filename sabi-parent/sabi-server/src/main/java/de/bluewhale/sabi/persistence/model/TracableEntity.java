/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import java.time.LocalDateTime;

/**
 *
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
public abstract class TracableEntity {

    LocalDateTime createdOn;

    @Column(name = "created_on", nullable = false, insertable = true, updatable = false)
    @Basic
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }


    LocalDateTime lastmodOn;

    @Column(name = "lastmod_on", nullable = true, insertable = true, updatable = true)
    @Basic
    public LocalDateTime getLastmodOn() {
        return lastmodOn;
    }

    public void setLastmodOn(LocalDateTime lastmodOn) {
        this.lastmodOn = lastmodOn;
    }

}
