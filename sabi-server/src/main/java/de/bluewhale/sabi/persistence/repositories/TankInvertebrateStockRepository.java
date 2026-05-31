/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.TankInvertebrateStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TankInvertebrateStockEntity.
 * {@code @SQLRestriction("deleted_at IS NULL")} on entity filters soft-deleted entries.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public interface TankInvertebrateStockRepository extends JpaRepository<TankInvertebrateStockEntity, Long> {

    /**
     * MANDATORY ownership check — always use this instead of findById for mutations.
     */
    @Query("SELECT i FROM TankInvertebrateStockEntity i WHERE i.id = :invertebrateId AND i.user.id = :userId")
    Optional<TankInvertebrateStockEntity> findByIdAndUserId(@Param("invertebrateId") Long invertebrateId, @Param("userId") Long userId);

    /**
     * All invertebrates (active + departed) in a tank filtered by user ownership (deleted_at IS NULL enforced by @SQLRestriction).
     */
    @Query("SELECT i FROM TankInvertebrateStockEntity i WHERE i.aquariumId = :aquariumId AND i.user.id = :userId ORDER BY i.addedOn DESC")
    List<TankInvertebrateStockEntity> findAllByAquariumIdAndUserId(@Param("aquariumId") Long aquariumId, @Param("userId") Long userId);

    /**
     * All invertebrates for a tank (non-soft-deleted) — used for public report and export.
     */
    @Query("SELECT i FROM TankInvertebrateStockEntity i WHERE i.aquariumId = :aquariumId AND i.user.id = :userId AND i.deletedAt IS NULL")
    List<TankInvertebrateStockEntity> findByAquariumIdAndUserIdAndDeletedAtIsNull(@Param("aquariumId") Long aquariumId, @Param("userId") Long userId);

    /**
     * Active invertebrates only (departedOn IS NULL) — used for public report.
     */
    @Query("SELECT i FROM TankInvertebrateStockEntity i WHERE i.aquariumId = :aquariumId AND i.departedOn IS NULL")
    List<TankInvertebrateStockEntity> findActiveByAquariumId(@Param("aquariumId") Long aquariumId);

    /**
     * Soft-delete all invertebrate entries of an aquarium.
     */
    @Modifying
    @Query("UPDATE TankInvertebrateStockEntity i SET i.deletedAt = :now WHERE i.aquariumId = :aquariumId")
    void softDeleteAllByAquariumId(@Param("aquariumId") Long aquariumId, @Param("now") LocalDateTime now);
}
