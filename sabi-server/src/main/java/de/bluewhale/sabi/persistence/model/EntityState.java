/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;


import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
@Embeddable
@Getter
@Setter
public class EntityState implements Serializable {

    @Column(name = "created_on", nullable = false, insertable = true, updatable = false)
    @Basic
    LocalDateTime createdOn;

    @Column(name = "lastmod_on", nullable = true, insertable = true, updatable = true)
    @Basic
    LocalDateTime lastmodOn;

}
