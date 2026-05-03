/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.TankFishStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TankFishStockEntity (fish table).
 * @SQLRestriction("deleted_at IS NULL") on entity filters soft-deleted entries automatically.
 *
 * @author Stefan Schubert
 */
public interface TankFishStockRepository extends JpaRepository<TankFishStockEntity, Long> {

    /**
     * All active (non-departed) fish in a tank, ordered by entry date desc.
     */
    @Query("SELECT f FROM TankFishStockEntity f WHERE f.aquariumId = :aquariumId " +
           "AND :userId IN (SELECT a.user.id FROM AquariumEntity a WHERE a.id = f.aquariumId) " +
           "ORDER BY f.addedOn DESC")
    List<TankFishStockEntity> findAllByAquariumIdAndUserIdOrderByAddedOnDesc(
            @Param("aquariumId") Long aquariumId, @Param("userId") Long userId);

    /**
     * Fetch a fish entry only if it belongs to the given user (ownership-check, FR-011).
     */
    @Query("SELECT f FROM TankFishStockEntity f WHERE f.id = :fishId " +
           "AND :userId IN (SELECT a.user.id FROM AquariumEntity a WHERE a.id = f.aquariumId)")
    Optional<TankFishStockEntity> findByIdAndUserId(@Param("fishId") Long fishId, @Param("userId") Long userId);

    /**
     * Guard for FR-024: returns true if the fish has a departure record.
     */
    @Query("SELECT CASE WHEN f.exodusOn IS NOT NULL THEN true ELSE false END " +
           "FROM TankFishStockEntity f WHERE f.id = :fishId")
    boolean existsByIdAndExodusOnIsNotNull(@Param("fishId") Long fishId);

    /**
     * Find all fish (including departed) for a tank — used for building full list in UI.
     */
    @Query("SELECT f FROM TankFishStockEntity f WHERE f.aquariumId = :aquariumId")
    List<TankFishStockEntity> findAllByAquariumId(@Param("aquariumId") Long aquariumId);

    /**
     * Soft-delete all fish entries of an aquarium (R-5, T075).
     */
    @Modifying
    @Query("UPDATE TankFishStockEntity f SET f.deletedAt = :now WHERE f.aquariumId = :aquariumId")
    void softDeleteAllByAquariumId(@Param("aquariumId") Long aquariumId, @Param("now") LocalDateTime now);

}

