/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.scheduler;

import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.services.MeasurementReminderEmail;

import java.time.LocalDate;
import java.util.Optional;

/**
 * SPI for reminder email content providers (sabi-69, FR-011).
 *
 * <p>The {@code ReminderScheduler} injects <b>all</b> registered provider beans
 * (constructor-injected {@code List<ReminderContentProvider>}) and, for every due
 * user/cycle, asks each provider for its payload. Every non-empty payload results in
 * one email per user and cycle.</p>
 *
 * <h2>How to add a new reminder type</h2>
 * <ol>
 *   <li>Create a new {@code @Component} implementing this interface with a stable,
 *       upper-case {@link #type()} identifier (e.g. {@code "MAINTENANCE"}).</li>
 *   <li>Implement {@link #buildContent(UserEntity, LocalDate)}: return the email
 *       payload for one user/cycle, or {@link Optional#empty()} when there is nothing
 *       to send (R6: no email, no delivery record, next send date still advances).</li>
 *   <li>Add the provider's own template keys to the i18n bundles (all supported
 *       languages) and build the payload/links inside the provider.</li>
 * </ol>
 * That is all — the scheduler picks the bean up automatically. No changes to the
 * scheduler core or to existing providers are needed (SC-006).
 * See {@code ReminderSchedulerExtensibilityTest} for a working example with a second
 * provider of type "MAINTENANCE".
 *
 * @author Stefan Schubert
 */
public interface ReminderContentProvider {

    /**
     * Stable type identifier of this reminder content, e.g. "MEASUREMENT".
     * Used for logging and future per-type delivery records.
     *
     * @return the type identifier, never null
     */
    String type();

    /**
     * Builds the email payload for one user and cycle.
     *
     * @param user      the due user to build content for
     * @param cycleDate the due date of the cycle (evaluation point)
     * @return the email payload, or empty when there is nothing to send for this
     *         user/cycle (R6: no email, no delivery record, next send date still advances)
     */
    Optional<MeasurementReminderEmail> buildContent(UserEntity user, LocalDate cycleDate);

}
