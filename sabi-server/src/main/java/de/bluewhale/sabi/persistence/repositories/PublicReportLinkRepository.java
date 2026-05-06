/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.PublicReportLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for public HouseReef report share links.
 *
 * @author Stefan Schubert
 */
public interface PublicReportLinkRepository extends JpaRepository<PublicReportLinkEntity, Long> {

    Optional<PublicReportLinkEntity> findByAquariumId(Long aquariumId);

    Optional<PublicReportLinkEntity> findByLinkToken(String linkToken);

    void deleteByAquariumId(Long aquariumId);
}
