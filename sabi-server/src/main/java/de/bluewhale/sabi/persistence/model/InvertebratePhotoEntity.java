/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Metadata for invertebrate photos (actual bytes stored on filesystem).
 * One photo per invertebrate entry (unique key on invertebrate_stock_id).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Table(name = "invertebrate_photo", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"invertebrateStock"})
@ToString(exclude = {"invertebrateStock"})
public class InvertebratePhotoEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "invertebrate_stock_id", nullable = false, unique = true)
    @Basic
    private Long invertebrateStockId;

    @Column(name = "file_path", nullable = false, length = 512)
    @Basic
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 50)
    @Basic
    private String contentType;

    @Column(name = "upload_date", nullable = false)
    @Basic
    private LocalDate uploadDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invertebrate_stock_id", insertable = false, updatable = false)
    private TankInvertebrateStockEntity invertebrateStock;
}
