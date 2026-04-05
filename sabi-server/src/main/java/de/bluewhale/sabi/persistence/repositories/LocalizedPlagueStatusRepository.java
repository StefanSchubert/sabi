/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.LocalizedPlagueStatusEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * SpringDataRepository for LocalizedPlagueStatusEntity.
 * Uses a JPQL query to avoid ambiguity with the snake_case field name plague_status_id.
 *
 * @author Stefan Schubert
 */
public interface LocalizedPlagueStatusRepository extends JpaRepository<LocalizedPlagueStatusEntity, Long> {

    /**
     * Returns the localized plague status entry for the given language and plague status ID.
     *
     * @param language     2-letter language code, e.g. "en"
     * @param plagueStatusId raw plague status catalogue ID
     * @return matching entity, or null if not found
     */
    @Query("SELECT l FROM LocalizedPlagueStatusEntity l WHERE l.language = :language AND l.plague_status_id = :plagueStatusId")
    LocalizedPlagueStatusEntity findByLanguageAndPlagueStatusId(@NotNull @Param("language") String language,
                                                                  @NotNull @Param("plagueStatusId") Integer plagueStatusId);
}
