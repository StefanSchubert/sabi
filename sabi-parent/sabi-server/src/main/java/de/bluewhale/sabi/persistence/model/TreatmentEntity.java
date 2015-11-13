package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "treatment", schema = "sabi")
@Entity
public class TreatmentEntity extends TracableEntity {

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

    private Timestamp givenOn;

    @javax.persistence.Column(name = "given_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getGivenOn() {
        return givenOn;
    }

    public void setGivenOn(Timestamp givenOn) {
        this.givenOn = givenOn;
    }

    private float amount;

    @javax.persistence.Column(name = "amount", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    private int unitId;

    @javax.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    private long remedyId;

    @javax.persistence.Column(name = "remedy_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getRemedyId() {
        return remedyId;
    }

    public void setRemedyId(long remedyId) {
        this.remedyId = remedyId;
    }

    private String description;

    @javax.persistence.Column(name = "description", nullable = true, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TreatmentEntity that = (TreatmentEntity) o;

        if (Float.compare(that.amount, amount) != 0) return false;
        if (aquariumId != that.aquariumId) return false;
        if (id != that.id) return false;
        if (remedyId != that.remedyId) return false;
        if (unitId != that.unitId) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (givenOn != null ? !givenOn.equals(that.givenOn) : that.givenOn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        result = 31 * result + (givenOn != null ? givenOn.hashCode() : 0);
        result = 31 * result + (amount != +0.0f ? Float.floatToIntBits(amount) : 0);
        result = 31 * result + unitId;
        result = 31 * result + (int) (remedyId ^ (remedyId >>> 32));
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
