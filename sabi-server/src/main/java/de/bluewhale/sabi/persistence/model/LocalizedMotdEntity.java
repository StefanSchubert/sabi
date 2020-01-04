/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 * Table which contains the translated message of today content.
 */
@Table(name = "localized_motd", schema = "sabi")
@Entity
public class LocalizedMotdEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long motd_id;

    private String language;

    private String text;

// --------------------- GETTER / SETTER METHODS ---------------------

    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "language", nullable = true, insertable = true, updatable = true, length = 3)
    @Basic
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Column(name = "motd_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getMotd_id() {
        return this.motd_id;
    }

    public void setMotd_id(final Long motd_id) {
        this.motd_id = motd_id;
    }

    @Column(name = "text", nullable = true, insertable = true, updatable = true, length = 255)
    @Basic
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalizedMotdEntity)) return false;

        final LocalizedMotdEntity that = (LocalizedMotdEntity) o;

        if (!this.id.equals(that.id)) return false;
        if (!this.motd_id.equals(that.motd_id)) return false;
        return this.language.equals(that.language);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.motd_id.hashCode();
        result = 31 * result + this.language.hashCode();
        return result;
    }
}
