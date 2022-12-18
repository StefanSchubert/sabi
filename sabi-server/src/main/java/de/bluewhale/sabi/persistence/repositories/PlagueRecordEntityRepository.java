/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.PlagueRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlagueRecordEntityRepository extends JpaRepository<PlagueRecordEntity, Long> {

    @NotNull
    List<PlagueRecordEntity> findByUserOrderByObservedOnDesc(@NotNull UserEntity user);

    /**
     * Used to get an overview of users plague records.
     * @param user, i.e. owner of the plague record
     * @param pageable, defines how many results should be retrieved (pageble) and how to be sorted,
     *                  example <i>Pageable page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "observedOn"));</i>
     *                  <b>UPDATE:</b> Page sorting doesn't seem to work so we sort directly
     * @return List of PlagueRecords, that belong to the User.
     */
    @NotNull List<PlagueRecordEntity> findByUserOrderByObservedOnDesc(@NotNull UserEntity user, @NotNull Pageable pageable);

    /**
     * Used e.g. for sanity checks, to check if the intervall matches the plague when adding a new record.
     * @param user, i.e. owner of the plague record
     * @param pPlagueIntervallId technical intervall id
     * @return List of PlagueRecords, that belong to the User and matches the param criteria.
     */
    @NotNull List<PlagueRecordEntity> findByUserAndPlagueIntervallId(@NotNull UserEntity user, @NotNull Integer pPlagueIntervallId);

}