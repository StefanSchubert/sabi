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
@Table(name = "coral", schema = "", catalog = "sabi")
@Entity
public class CoralEntity {

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

    private long coralCatalougeId;

    @javax.persistence.Column(name = "coral_catalouge_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getCoralCatalougeId() {
        return coralCatalougeId;
    }

    public void setCoralCatalougeId(long coralCatalougeId) {
        this.coralCatalougeId = coralCatalougeId;
    }

    private Timestamp added_On;

    @javax.persistence.Column(name = "added_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getAdded_On() {
        return added_On;
    }

    public void setAdded_On(Timestamp added_On) {
        this.added_On = added_On;
    }

    private Timestamp exodus_On;

    @javax.persistence.Column(name = "exodus_on", nullable = true, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getExodus_On() {
        return exodus_On;
    }

    public void setExodus_On(Timestamp exodus_On) {
        this.exodus_On = exodus_On;
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

    private String observed_Behavior;

    @javax.persistence.Column(name = "observed_behavior", nullable = true, insertable = true, updatable = true, length = 65535, precision = 0)
    @Basic
    public String getObserved_Behavior() {
        return observed_Behavior;
    }

    public void setObserved_Behavior(String observed_Behavior) {
        this.observed_Behavior = observed_Behavior;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoralEntity that = (CoralEntity) o;

        if (aquariumId != that.aquariumId) return false;
        if (coralCatalougeId != that.coralCatalougeId) return false;
        if (id != that.id) return false;
        if (added_On != null ? !added_On.equals(that.added_On) : that.added_On != null) return false;
        if (exodus_On != null ? !exodus_On.equals(that.exodus_On) : that.exodus_On != null) return false;
        if (nickname != null ? !nickname.equals(that.nickname) : that.nickname != null) return false;
        if (observed_Behavior != null ? !observed_Behavior.equals(that.observed_Behavior) : that.observed_Behavior != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        result = 31 * result + (int) (coralCatalougeId ^ (coralCatalougeId >>> 32));
        result = 31 * result + (added_On != null ? added_On.hashCode() : 0);
        result = 31 * result + (exodus_On != null ? exodus_On.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        result = 31 * result + (observed_Behavior != null ? observed_Behavior.hashCode() : 0);
        return result;
    }
}
