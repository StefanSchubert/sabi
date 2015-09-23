package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@Table(name = "aquarium", schema = "", catalog = "sabi")
@Entity
public class AquariumEntity extends TracableEntity {

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

    private Integer size;

    @javax.persistence.Column(name = "size", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    private String size_Unit;

    @javax.persistence.Column(name = "size_unit", nullable = true, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public String getSize_Unit() {
        return size_Unit;
    }

    public void setSize_Unit(String size_Unit) {
        this.size_Unit = size_Unit;
    }

    private String validate_Token;

    @javax.persistence.Column(name = "validate_token", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getValidate_Token() {
        return validate_Token;
    }

    public void setValidate_Token(String validate_Token) {
        this.validate_Token = validate_Token;
    }

    private Boolean active;

    @javax.persistence.Column(name = "active", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    private Long user_Id;

    @javax.persistence.Column(name = "user_id", nullable = true, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getUser_Id() {
        return user_Id;
    }

    public void setUser_Id(Long user_Id) {
        this.user_Id = user_Id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AquariumEntity that = (AquariumEntity) o;

        if (id != that.id) return false;
        if (active != null ? !active.equals(that.active) : that.active != null) return false;
        if (created_On != null ? !created_On.equals(that.created_On) : that.created_On != null) return false;
        if (lastmod_On != null ? !lastmod_On.equals(that.lastmod_On) : that.lastmod_On != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (size_Unit != null ? !size_Unit.equals(that.size_Unit) : that.size_Unit != null) return false;
        if (user_Id != null ? !user_Id.equals(that.user_Id) : that.user_Id != null) return false;
        if (validate_Token != null ? !validate_Token.equals(that.validate_Token) : that.validate_Token != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (size_Unit != null ? size_Unit.hashCode() : 0);
        result = 31 * result + (validate_Token != null ? validate_Token.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        result = 31 * result + (user_Id != null ? user_Id.hashCode() : 0);
        result = 31 * result + (created_On != null ? created_On.hashCode() : 0);
        result = 31 * result + (lastmod_On != null ? lastmod_On.hashCode() : 0);
        return result;
    }
}
