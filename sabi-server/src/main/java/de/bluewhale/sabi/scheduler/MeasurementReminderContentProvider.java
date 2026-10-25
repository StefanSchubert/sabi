/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.scheduler;

import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.services.EmailReminderService;
import de.bluewhale.sabi.services.MeasurementReminderEmail;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * First {@link ReminderContentProvider} implementation: the measurement reminder (sabi-69).
 * Applies the overdue rule from FR-008 via {@link EmailReminderService#buildEmailContent}
 * and returns the deep links built from {@code sabi.email.base-url}.
 *
 * @author Stefan Schubert
 */
@Component
public class MeasurementReminderContentProvider implements ReminderContentProvider {

    public static final String TYPE_MEASUREMENT = "MEASUREMENT";

    private final EmailReminderService emailReminderService;

    public MeasurementReminderContentProvider(EmailReminderService emailReminderService) {
        this.emailReminderService = emailReminderService;
    }

    @Override
    public String type() {
        return TYPE_MEASUREMENT;
    }

    @Override
    public Optional<MeasurementReminderEmail> buildContent(UserEntity user, LocalDate cycleDate) {
        return emailReminderService.buildEmailContent(user, cycleDate);
    }

}
