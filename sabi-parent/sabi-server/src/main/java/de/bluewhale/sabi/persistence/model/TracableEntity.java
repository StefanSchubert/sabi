/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;


/**
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
public abstract class TracableEntity {

    // To be used with @Embedded EntityStatus
    public abstract EntityState getEntityState();
    public abstract void setEntityState(EntityState entityState);

}
