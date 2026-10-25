/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transient payload for one measurement reminder email (NOT persisted).
 * Built at send time by {@link EmailReminderService}, consumed by
 * {@code NotificationServiceImpl.sendMeasurementReminderEmail(...)}.
 *
 * @param userId              the owning user's ID (used for logging: user_id= only, PII rule)
 * @param emailAddress        the user's verified email address (To: header)
 * @param username            the user's display name for the localized greeting ({0} placeholder)
 * @param languageCode        the user's ISO-639-1 language code for bundle/locale selection
 * @param overdueUnits        the active in-app reminder units that are currently overdue (FR-008)
 * @param measurementPageUrl  deep link to the measurement page
 * @param profilePageUrl      deep link to the profile settings page
 * @author Stefan Schubert
 */
public record MeasurementReminderEmail(
        Long userId,
        String emailAddress,
        String username,
        String languageCode,
        List<OverdueUnit> overdueUnits,
        String measurementPageUrl,
        String profilePageUrl) {

    /**
     * One overdue unit row of the email table.
     *
     * @param unitSign        e.g. "NO3 (mg/l)" — from the unit catalogue
     * @param lastValue       last measured value; NULL when never measured
     * @param lastMeasuredOn  last measurement timestamp; NULL when never measured
     */
    public record OverdueUnit(String unitSign, Float lastValue, LocalDateTime lastMeasuredOn) {
    }

}
