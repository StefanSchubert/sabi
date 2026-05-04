/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Metadata for aquarium photos (actual bytes stored on filesystem).
 *
 * @author Stefan Schubert
 */
@Table(name = "aquarium_photo", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class AquariumPhotoEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false, unique = true)
    @Basic
    private Long aquariumId;

    @Column(name = "file_path", nullable = false, length = 512)
    @Basic
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 50)
    @Basic
    private String contentType;

    @Column(name = "file_size", nullable = false)
    @Basic
    private Long fileSize;

    @Column(name = "upload_date", nullable = false)
    @Basic
    private LocalDate uploadDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aquarium_id", insertable = false, updatable = false)
    private AquariumEntity aquarium;

}

