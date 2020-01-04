/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Table which contains the message of today content.
 */
@NamedQueries({@NamedQuery(name = "Motd.getValidModt",
        query = "select a from MotdEntity a where a.publishDate <= current_timestamp " +
                "and (a.vanishDate is null or a.vanishDate > current_timestamp")})
@Table(name = "motd", schema = "sabi")
@Entity
public class MotdEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    // Unidirectional for now - as this contains more static data, we we won't provide a admin gui for it.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="motd_id")
    private List<LocalizedMotdEntity> localizedMotdEntities = new ArrayList<LocalizedMotdEntity>();

    @Basic
    @Column(name = "publish_date", nullable = false)
    protected LocalDateTime publishDate;

    @Basic
    @Column(name = "vanish_date", nullable = true)
    protected LocalDateTime vanishDate;

// --------------------- GETTER / SETTER METHODS ---------------------


    public List<LocalizedMotdEntity> getLocalizedMotdEntities() {
        return this.localizedMotdEntities;
    }

    public void setLocalizedMotdEntities(final List<LocalizedMotdEntity> localizedMotdEntities) {
        this.localizedMotdEntities = localizedMotdEntities;
    }

    public LocalDateTime getPublishDate() {
        return this.publishDate;
    }

    public void setPublishDate(final LocalDateTime publishDate) {
        this.publishDate = publishDate;
    }

    public LocalDateTime getVanishDate() {
        return this.vanishDate;
    }

    public void setVanishDate(final LocalDateTime vanishDate) {
        this.vanishDate = vanishDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MotdEntity)) return false;

        final MotdEntity that = (MotdEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.localizedMotdEntities.equals(that.localizedMotdEntities)) return false;
        if (!this.publishDate.equals(that.publishDate)) return false;
        return this.vanishDate != null ? this.vanishDate.equals(that.vanishDate) : that.vanishDate == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.localizedMotdEntities.hashCode();
        result = 31 * result + this.publishDate.hashCode();
        result = 31 * result + (this.vanishDate != null ? this.vanishDate.hashCode() : 0);
        return result;
    }
}
