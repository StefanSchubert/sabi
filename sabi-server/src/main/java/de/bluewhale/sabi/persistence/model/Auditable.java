/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;


@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Basic
    @Column(name = "created_on", nullable = false, insertable = true, updatable = false)
    protected  LocalDateTime createdOn;


    @LastModifiedDate
    @Basic
    @Column(name = "lastmod_on", nullable = true, insertable = true, updatable = true)
    protected LocalDateTime lastmodOn;


    @Version
    @Column(name = "optlock", columnDefinition = "integer DEFAULT 0", nullable = false)
    private long optlock = 0L;


    public LocalDateTime getCreatedOn() {
        return this.createdOn;
    }

    public LocalDateTime getLastmodOn() {
        return this.lastmodOn;
    }
}
