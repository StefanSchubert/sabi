package de.bluewhale.sabi.persistence;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@javax.persistence.Table(name = "users", schema = "", catalog = "sabi")
@Entity
public class UsersEntity {

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

    private String email;

    @javax.persistence.Column(name = "email", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private String password;

    @javax.persistence.Column(name = "password", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String validateToken;

    @javax.persistence.Column(name = "validate_token", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getValidateToken() {
        return validateToken;
    }

    public void setValidateToken(String validateToken) {
        this.validateToken = validateToken;
    }

    private boolean validated;

    @javax.persistence.Column(name = "validated", nullable = false, insertable = true, updatable = true, length = 3, precision = 0)
    @Basic
    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    private Timestamp createdOn;

    @javax.persistence.Column(name = "created_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    private Timestamp lastmodOn;

    @javax.persistence.Column(name = "lastmod_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getLastmodOn() {
        return lastmodOn;
    }

    public void setLastmodOn(Timestamp lastmodOn) {
        this.lastmodOn = lastmodOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsersEntity that = (UsersEntity) o;

        if (id != that.id) return false;
        if (validated != that.validated) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (lastmodOn != null ? !lastmodOn.equals(that.lastmodOn) : that.lastmodOn != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (validateToken != null ? !validateToken.equals(that.validateToken) : that.validateToken != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (validateToken != null ? validateToken.hashCode() : 0);
        result = 31 * result + (validated ? 1 : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        result = 31 * result + (lastmodOn != null ? lastmodOn.hashCode() : 0);
        return result;
    }
}
