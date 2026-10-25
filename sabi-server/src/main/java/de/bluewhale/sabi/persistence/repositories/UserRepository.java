/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.UserEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Returns an User object that matches the email given
     *
     * @param email
     * @return
     */
    UserEntity getByEmail(@NotNull String email);

    /**
     * Returns an User object that matches the username given
     *
     * @param username
     * @return
     */
    UserEntity getByUsername(@NotNull String username);


    /**
     * Checks if we have an user with this email
     * @param pEmail
     * @return
     */
    boolean existsUserEntityByEmailEquals(@NotNull String pEmail);

    /**
     * Finds all users whose email measurement reminders are enabled and whose next
     * send date is due (on or before the given run date). Serves the daily scheduler
     * batch; backed by idx_users_email_reminder_due.
     *
     * @param runDate the evaluation date of the scheduler run
     * @return due users, possibly empty
     */
    List<UserEntity> findByEmailReminderEnabledTrueAndEmailReminderNextSendDateLessThanEqual(LocalDate runDate);
}
