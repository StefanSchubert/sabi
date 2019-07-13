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
@NamedQueries({@NamedQuery(name="Fish.getUsersFish",
        query="select f from FishEntity f where :pUserId in (select a.user.id from AquariumEntity a where a.id = f.aquariumId) and f.id = :pFishId")})
@Table(name = "fish", schema = "sabi")
@Entity
public class FishEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long aquariumId;

    private Long fishCatalogueId;

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


    @javax.persistence.Column(name = "exodus_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getExodusOn() {
        return exodusOn;
    }

    public void setExodusOn(Timestamp exodusOn) {
        this.exodusOn = exodusOn;
    }

    @javax.persistence.Column(name = "fish_catalogue_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getFishCatalogueId() {
        return fishCatalogueId;
    }

    public void setFishCatalogueId(Long fishCatalogueId) {
        this.fishCatalogueId = fishCatalogueId;
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

        FishEntity that = (FishEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.aquariumId.equals(that.aquariumId)) return false;
        if (this.fishCatalogueId != null ? !this.fishCatalogueId.equals(that.fishCatalogueId) : that.fishCatalogueId != null)
            return false;
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
        result = 31 * result + (this.fishCatalogueId != null ? this.fishCatalogueId.hashCode() : 0);
        result = 31 * result + (this.addedOn != null ? this.addedOn.hashCode() : 0);
        result = 31 * result + (this.exodusOn != null ? this.exodusOn.hashCode() : 0);
        result = 31 * result + (this.nickname != null ? this.nickname.hashCode() : 0);
        result = 31 * result + (this.observedBehavior != null ? this.observedBehavior.hashCode() : 0);
        result = 31 * result + this.user.hashCode();
        return result;
    }
}
