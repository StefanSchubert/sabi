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
 * Metadata for coral photos (actual bytes stored on filesystem, C-4).
 * One photo per coral entry (unique key on coral_stock_id).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Table(name = "coral_photo", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"coralStock"})
@ToString(exclude = {"coralStock"})
public class CoralPhotoEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "coral_stock_id", nullable = false, unique = true)
    @Basic
    private Long coralStockId;

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
    @JoinColumn(name = "coral_stock_id", insertable = false, updatable = false)
    private TankCoralStockEntity coralStock;
}

