/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.TankCoralStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TankCoralStockEntity.
 * {@code @SQLRestriction("deleted_at IS NULL")} on entity filters soft-deleted entries.
 *
 * @author Stefan Schubert
 */
public interface TankCoralStockRepository extends JpaRepository<TankCoralStockEntity, Long> {

    /**
     * MANDATORY ownership check — always use this instead of findById for mutations.
     */
    @Query("SELECT c FROM TankCoralStockEntity c WHERE c.id = :coralId AND c.user.id = :userId")
    Optional<TankCoralStockEntity> findByIdAndUserId(@Param("coralId") Long coralId, @Param("userId") Long userId);

    /**
     * All active corals in a tank (deleted_at IS NULL enforced by @SQLRestriction).
     */
    @Query("SELECT c FROM TankCoralStockEntity c WHERE c.aquariumId = :aquariumId ORDER BY c.addedOn DESC")
    List<TankCoralStockEntity> findByAquariumIdOrderByAddedOnDesc(@Param("aquariumId") Long aquariumId);

    /**
     * All corals for a tank including departed (filters only soft-deleted).
     */
    List<TankCoralStockEntity> findByAquariumIdAndDeletedAtIsNull(Long aquariumId);

    /**
     * Soft-delete all coral entries of an aquarium.
     */
    @Modifying
    @Query("UPDATE TankCoralStockEntity c SET c.deletedAt = :now WHERE c.aquariumId = :aquariumId")
    void softDeleteAllByAquariumId(@Param("aquariumId") Long aquariumId, @Param("now") LocalDateTime now);

    /**
     * Active corals only (departedOn IS NULL) — used for public report.
     */
    @Query("SELECT c FROM TankCoralStockEntity c WHERE c.aquariumId = :aquariumId AND c.departedOn IS NULL")
    List<TankCoralStockEntity> findActiveByAquariumId(@Param("aquariumId") Long aquariumId);
}

