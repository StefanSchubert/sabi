/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;


import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
@Embeddable
public class EntityState implements Serializable {

    LocalDateTime createdOn;
    LocalDateTime lastmodOn;

    @Column(name = "created_on", nullable = false, insertable = true, updatable = false)
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    @Column(name = "lastmod_on", nullable = true, insertable = true, updatable = true)
    @Basic
    public LocalDateTime getLastmodOn() {
        return lastmodOn;
    }

    public void setLastmodOn(LocalDateTime lastmodOn) {
        this.lastmodOn = lastmodOn;
    }

}