/*
 * Copyright (c) 2019 by Stefan Schubert
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
public class CoralEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long aquariumId;

    private long coralCatalougeId;

    private Timestamp addedOn;

    private Timestamp exodusOn;

    private String nickname;

    private String observedBehavior;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "added_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(Timestamp addedOn) {
        this.addedOn = addedOn;
    }

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @javax.persistence.Column(name = "coral_catalouge_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getCoralCatalougeId() {
        return coralCatalougeId;
    }

    public void setCoralCatalougeId(Long coralCatalougeId) {
        this.coralCatalougeId = coralCatalougeId;
    }


    @javax.persistence.Column(name = "exodus_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getExodusOn() {
        return exodusOn;
    }

    public void setExodusOn(Timestamp exodusOn) {
        this.exodusOn = exodusOn;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "nickname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @javax.persistence.Column(name = "observed_behavior", nullable = true, insertable = true, updatable = true, length = 65535, precision = 0)
    @Basic
    public String getObservedBehavior() {
        return observedBehavior;
    }

    public void setObservedBehavior(String observedBehavior) {
        this.observedBehavior = observedBehavior;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        CoralEntity that = (CoralEntity) o;

        if (this.coralCatalougeId != that.coralCatalougeId) return false;
        if (!this.id.equals(that.id)) return false;
        if (!this.aquariumId.equals(that.aquariumId)) return false;
        if (this.addedOn != null ? !this.addedOn.equals(that.addedOn) : that.addedOn != null) return false;
        if (this.exodusOn != null ? !this.exodusOn.equals(that.exodusOn) : that.exodusOn != null) return false;
        if (this.nickname != null ? !this.nickname.equals(that.nickname) : that.nickname != null) return false;
        if (this.observedBehavior != null ? !this.observedBehavior.equals(that.observedBehavior) : that.observedBehavior != null)
            return false;
        return this.user.equals(that.user);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.aquariumId.hashCode();
        result = 31 * result + (int) (this.coralCatalougeId ^ this.coralCatalougeId >>> 32);
        result = 31 * result + (this.addedOn != null ? this.addedOn.hashCode() : 0);
        result = 31 * result + (this.exodusOn != null ? this.exodusOn.hashCode() : 0);
        result = 31 * result + (this.nickname != null ? this.nickname.hashCode() : 0);
        result = 31 * result + (this.observedBehavior != null ? this.observedBehavior.hashCode() : 0);
        result = 31 * result + this.user.hashCode();
        return result;
    }
}
