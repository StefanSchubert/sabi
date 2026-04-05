/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.LocalizedPlagueEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * SpringDataRepository for LocalizedPlagueEntity.
 * Uses a JPQL query to avoid ambiguity with the snake_case field name plague_id.
 *
 * @author Stefan Schubert
 */
public interface LocalizedPlagueRepository extends JpaRepository<LocalizedPlagueEntity, Long> {

    /**
     * Returns the localized plague entry for the given language and plague ID.
     *
     * @param language 2-letter language code, e.g. "en"
     * @param plagueId raw plague catalogue ID
     * @return matching entity, or null if not found
     */
    @Query("SELECT l FROM LocalizedPlagueEntity l WHERE l.language = :language AND l.plague_id = :plagueId")
    LocalizedPlagueEntity findByLanguageAndPlagueId(@NotNull @Param("language") String language,
                                                     @NotNull @Param("plagueId") Integer plagueId);
}
