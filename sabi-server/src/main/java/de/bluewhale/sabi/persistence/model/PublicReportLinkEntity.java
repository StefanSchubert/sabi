/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Represents a public share link for a HouseReef report.
 * One active link per aquarium (enforced by unique constraint on aquarium_id).
 *
 * @author Stefan Schubert
 */
@Table(name = "public_report_link", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class PublicReportLinkEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false, unique = true)
    @Basic
    private Long aquariumId;

    @Column(name = "link_token", nullable = false, unique = true, length = 36)
    @Basic
    private String linkToken;

    @Column(name = "valid_until", nullable = true)
    @Basic
    private LocalDateTime validUntil;

}
