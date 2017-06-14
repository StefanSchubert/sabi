/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "coral", schema = "sabi")
@Entity
public class CoralEntity extends TracableEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private Long aquariumId;

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    private long coralCatalougeId;

    @javax.persistence.Column(name = "coral_catalouge_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getCoralCatalougeId() {
        return coralCatalougeId;
    }

    public void setCoralCatalougeId(Long coralCatalougeId) {
        this.coralCatalougeId = coralCatalougeId;
    }

    private Timestamp addedOn;

    @javax.persistence.Column(name = "added_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(Timestamp addedOn) {
        this.addedOn = addedOn;
    }

    private Timestamp exodusOn;

    @javax.persistence.Column(name = "exodus_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getExodusOn() {
        return exodusOn;
    }

    public void setExodusOn(Timestamp exodusOn) {
        this.exodusOn = exodusOn;
    }

    private String nickname;

    @javax.persistence.Column(name = "nickname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    private String observedBehavior;

    @javax.persistence.Column(name = "observed_behavior", nullable = true, insertable = true, updatable = true, length = 65535, precision = 0)
    @Basic
    public String getObservedBehavior() {
        return observedBehavior;
    }

    public void setObservedBehavior(String observedBehavior) {
        this.observedBehavior = observedBehavior;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoralEntity that = (CoralEntity) o;

        if (aquariumId != that.aquariumId) return false;
        if (coralCatalougeId != that.coralCatalougeId) return false;
        if (id != that.id) return false;
        if (addedOn != null ? !addedOn.equals(that.addedOn) : that.addedOn != null) return false;
        if (exodusOn != null ? !exodusOn.equals(that.exodusOn) : that.exodusOn != null) return false;
        if (nickname != null ? !nickname.equals(that.nickname) : that.nickname != null) return false;
        if (observedBehavior != null ? !observedBehavior.equals(that.observedBehavior) : that.observedBehavior != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        result = 31 * result + (int) (coralCatalougeId ^ (coralCatalougeId >>> 32));
        result = 31 * result + (addedOn != null ? addedOn.hashCode() : 0);
        result = 31 * result + (exodusOn != null ? exodusOn.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        result = 31 * result + (observedBehavior != null ? observedBehavior.hashCode() : 0);
        return result;
    }
}
