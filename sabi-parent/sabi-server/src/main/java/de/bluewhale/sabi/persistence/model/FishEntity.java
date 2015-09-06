package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@Table(name = "fish", schema = "", catalog = "sabi")
@Entity
public class FishEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long id;

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private long aquariumId;

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(long aquariumId) {
        this.aquariumId = aquariumId;
    }

    private long fishCatalougeId;

    @javax.persistence.Column(name = "fish_catalouge_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getFishCatalougeId() {
        return fishCatalougeId;
    }

    public void setFishCatalougeId(long fishCatalougeId) {
        this.fishCatalougeId = fishCatalougeId;
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

    private String observedBahavior;

    @javax.persistence.Column(name = "observed_behavior", nullable = true, insertable = true, updatable = true, length = 65535, precision = 0)
    @Basic
    public String getObservedBahavior() {
        return observedBahavior;
    }

    public void setObservedBahavior(String observedBahavior) {
        this.observedBahavior = observedBahavior;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FishEntity that = (FishEntity) o;

        if (aquariumId != that.aquariumId) return false;
        if (fishCatalougeId != that.fishCatalougeId) return false;
        if (id != that.id) return false;
        if (addedOn != null ? !addedOn.equals(that.addedOn) : that.addedOn != null) return false;
        if (exodusOn != null ? !exodusOn.equals(that.exodusOn) : that.exodusOn != null) return false;
        if (nickname != null ? !nickname.equals(that.nickname) : that.nickname != null) return false;
        if (observedBahavior != null ? !observedBahavior.equals(that.observedBahavior) : that.observedBahavior != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        result = 31 * result + (int) (fishCatalougeId ^ (fishCatalougeId >>> 32));
        result = 31 * result + (addedOn != null ? addedOn.hashCode() : 0);
        result = 31 * result + (exodusOn != null ? exodusOn.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        result = 31 * result + (observedBahavior != null ? observedBahavior.hashCode() : 0);
        return result;
    }
}
