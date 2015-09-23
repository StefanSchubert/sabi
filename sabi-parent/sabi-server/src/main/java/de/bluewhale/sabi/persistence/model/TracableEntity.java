package de.bluewhale.sabi.persistence.model;

import javax.persistence.Basic;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 23.09.15
 */
public abstract class TracableEntity {

    Timestamp created_On;

    @javax.persistence.Column(name = "created_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getCreated_On() {
        return created_On;
    }

    public void setCreated_On(Timestamp created_On) {
        this.created_On = created_On;
    }


    Timestamp lastmod_On;

    @javax.persistence.Column(name = "lastmod_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getLastmod_On() {
        return lastmod_On;
    }

    public void setLastmod_On(Timestamp lastmod_On) {
        this.lastmod_On = lastmod_On;
    }

}
