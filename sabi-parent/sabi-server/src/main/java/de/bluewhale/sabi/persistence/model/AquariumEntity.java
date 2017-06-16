/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.SizeUnit;

import javax.persistence.*;

@NamedQueries({@NamedQuery(name = "Aquarium.getAquarium",
        query = "select a from AquariumEntity a where a.id = :pTankId and a.user.id = :pUserID"),
        @NamedQuery(name = "Aquarium.getUsersAquariums",
                query = "select a FROM AquariumEntity a where a.user.id = :pUserID")})
@Table(name = "aquarium", schema = "sabi")
@Entity
public class AquariumEntity extends TracableEntity {
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
        if (o == null || getClass() != o.getClass()) return false;

        AquariumEntity that = (AquariumEntity) o;

        if (id != that.id) return false;
        if (active != null ? !active.equals(that.active) : that.active != null) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (lastmodOn != null ? !lastmodOn.equals(that.lastmodOn) : that.lastmodOn != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (sizeUnit != null ? !sizeUnit.equals(that.sizeUnit) : that.sizeUnit != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (sizeUnit != null ? sizeUnit.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        result = 31 * result + (lastmodOn != null ? lastmodOn.hashCode() : 0);
        return result;
    }
}
