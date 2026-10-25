/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.persistence.model.UserEntity;

import java.time.LocalDate;
import java.util.Optional;

/**
 * State machine and content builder for email measurement reminders (sabi-69).
 * The existing in-app reminder endpoints are NOT touched (R10).
 *
 * @author Stefan Schubert
 */
public interface EmailReminderService {

    /**
     * Applies the email reminder state machine to the given user (contracts/profile-api.md):
     * <ul>
     *   <li>enable with &ge; 1 overdue unit → nextSendDate = today (FR-001)</li>
     *   <li>enable without overdue units → nextSendDate = today + interval</li>
     *   <li>interval change while enabled → nextSendDate = today + newInterval (FR-009)</li>
     *   <li>disable → nextSendDate = NULL (FR-010)</li>
     * </ul>
     * A {@code null} interval means "unchanged/absent" (backward compatibility for old clients).
     *
     * @param user         the managed user entity (changes are flushed by the surrounding transaction)
     * @param enabled      requested opt-in flag
     * @param intervalDays requested cadence in days (1-30), or null for unchanged
     * @throws BusinessException if enabled and the interval is outside 1-30 (FR-002)
     */
    void applyProfileChange(UserEntity user, boolean enabled, Integer intervalDays) throws BusinessException;

    /**
     * Builds the email payload for one user/cycle using the overdue rule (FR-008):
     * only active in-app reminder units whose last measurement is older than their
     * configured interval, or that have never been measured.
     *
     * @param user      the user to build content for
     * @param cycleDate the due date of the cycle (evaluation point)
     * @return the email payload, or empty if there are no overdue units (R6: no send)
     */
    Optional<MeasurementReminderEmail> buildEmailContent(UserEntity user, LocalDate cycleDate);

}
