/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.SizeUnit;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@NamedQueries({@NamedQuery(name = "Aquarium.getAquarium",
        query = "select a from AquariumEntity a where a.id = :pTankId and a.user.id = :pUserID"),
        @NamedQuery(name = "Aquarium.getUsersAquariums",
                query = "select a FROM AquariumEntity a where a.user.id = :pUserID")})
@Table(name = "aquarium", schema = "sabi")
@Entity
public class AquariumEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    @Basic
    @Column(name = "size", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    private Integer size;

    @Column(name = "size_unit", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Enumerated(EnumType.STRING)
    private SizeUnit sizeUnit;

    @Basic
    @Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    private String description;

    @Column(name = "active", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    private Boolean active;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aquarium")
    private List<MeasurementEntity> measurements = new ArrayList<MeasurementEntity>();


// --------------------- GETTER / SETTER METHODS ---------------------

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String validateToken) {
        this.description = validateToken;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MeasurementEntity> getMeasurements() {
        return this.measurements;
    }

    public void setMeasurements(List<MeasurementEntity> measurements) {
        this.measurements = measurements;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public SizeUnit getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(SizeUnit sizeUnit) {
        this.sizeUnit = sizeUnit;
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

        AquariumEntity that = (AquariumEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (this.size != null ? !this.size.equals(that.size) : that.size != null) return false;
        if (this.sizeUnit != that.sizeUnit) return false;
        if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
        if (this.active != null ? !this.active.equals(that.active) : that.active != null) return false;
        if (!this.user.equals(that.user)) return false;
        return this.measurements != null ? this.measurements.equals(that.measurements) : that.measurements == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + (this.size != null ? this.size.hashCode() : 0);
        result = 31 * result + (this.sizeUnit != null ? this.sizeUnit.hashCode() : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + (this.active != null ? this.active.hashCode() : 0);
        result = 31 * result + this.user.hashCode();
        result = 31 * result + (this.measurements != null ? this.measurements.hashCode() : 0);
        return result;
    }
}
