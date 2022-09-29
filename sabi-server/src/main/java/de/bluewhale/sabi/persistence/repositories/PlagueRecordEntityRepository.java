/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.PlagueRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface PlagueRecordEntityRepository extends JpaRepository<PlagueRecordEntity, Long> {

    @NotNull
    List<PlagueRecordEntity> findByUserOrderByObservedOnDesc(@NotNull UserEntity user);

    /**
     * Used to get an overview of users plague records.
     * @param user, i.e. owner of the measurements
     * @param pageable, defines how many results should be retrieved (pageble) and how to be sorted,
     *                  example <i>Pageable page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "observedOn"));</i>
     *                  <b>UPDATE:</b> Page sorting doesn't seem to work so we sort directly
     * @return List of Measurements, that belong to the User.
     */
    @NotNull List<PlagueRecordEntity> findByUserOrderByObservedOnDesc(@NotNull UserEntity user, @NotNull Pageable pageable);

}