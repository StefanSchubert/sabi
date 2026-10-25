/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.UserProfileMessageCodes;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UnitEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UnitRepository;
import de.bluewhale.sabi.persistence.repositories.UserMeasurementReminderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the email reminder state machine and content builder (sabi-69).
 * The existing in-app reminder endpoints are NOT modified (R10).
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class EmailReminderServiceImpl implements EmailReminderService {

    public static final int MIN_INTERVAL_DAYS = 1;
    public static final int MAX_INTERVAL_DAYS = 30;
    public static final int DEFAULT_INTERVAL_DAYS = 7;

    private static final String MEASUREMENT_PAGE_PATH = "/secured/measureView.xhtml";
    private static final String PROFILE_PAGE_PATH = "/secured/userProfile.xhtml";

    private final Clock clock;
    private final MeasurementRepository measurementRepository;
    private final UnitRepository unitRepository;
    private final UserMeasurementReminderRepository userMeasurementReminderRepository;
    private final String emailBaseUrl;

    public EmailReminderServiceImpl(Clock clock,
                                    MeasurementRepository measurementRepository,
                                    UnitRepository unitRepository,
                                    UserMeasurementReminderRepository userMeasurementReminderRepository,
                                    @Value("${sabi.email.base-url}") String emailBaseUrl) {
        this.clock = clock;
        this.measurementRepository = measurementRepository;
        this.unitRepository = unitRepository;
        this.userMeasurementReminderRepository = userMeasurementReminderRepository;
        this.emailBaseUrl = emailBaseUrl;
    }

    @Override
    public void applyProfileChange(UserEntity user, boolean enabled, Integer intervalDays) throws BusinessException {
        // Server-side validation (FR-002): reject the whole profile update when enabled
        // with an out-of-range interval. null = "unchanged/absent" (old clients).
        if (enabled && intervalDays != null
                && (intervalDays < MIN_INTERVAL_DAYS || intervalDays > MAX_INTERVAL_DAYS)) {
            log.warn("Rejected email reminder interval {} for user_id={}", intervalDays, user.getId());
            throw BusinessException.with(UserProfileMessageCodes.EMAIL_REMINDER_INTERVAL_OUT_OF_RANGE, intervalDays);
        }

        LocalDate today = LocalDate.now(clock);
        boolean wasEnabled = user.isEmailReminderEnabled();
        Integer previousInterval = user.getEmailReminderIntervalDays();

        if (enabled) {
            int effectiveInterval = intervalDays != null ? intervalDays
                    : (previousInterval != null ? previousInterval : DEFAULT_INTERVAL_DAYS);

            if (!wasEnabled) {
                // enable transition (FR-001): overdue units → send today, otherwise next cycle
                boolean hasOverdueUnits = !findOverdueUnits(user, today).isEmpty();
                user.setEmailReminderNextSendDate(hasOverdueUnits ? today : today.plusDays(effectiveInterval));
            } else if (intervalDays != null && !intervalDays.equals(previousInterval)) {
                // interval change while enabled (FR-009): recompute from now
                user.setEmailReminderNextSendDate(today.plusDays(intervalDays));
            }

            user.setEmailReminderEnabled(true);
            user.setEmailReminderIntervalDays(effectiveInterval);
        } else {
            // disable (FR-010): stop all sends, clear the next send date
            user.setEmailReminderEnabled(false);
            user.setEmailReminderNextSendDate(null);
        }
    }

    @Override
    public Optional<MeasurementReminderEmail> buildEmailContent(UserEntity user, LocalDate cycleDate) {
        List<MeasurementReminderEmail.OverdueUnit> overdueUnits = findOverdueUnits(user, cycleDate);
        if (overdueUnits.isEmpty()) {
            // R6: nothing overdue → no email, no delivery record
            return Optional.empty();
        }
        return Optional.of(new MeasurementReminderEmail(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getLanguage(),
                overdueUnits,
                emailBaseUrl + MEASUREMENT_PAGE_PATH,
                emailBaseUrl + PROFILE_PAGE_PATH));
    }

    /**
     * Overdue rule (FR-008): an active in-app reminder unit is overdue when its last
     * measurement is older than the configured interval, or when it was never measured.
     *
     * @param user      the user whose active in-app reminders are evaluated
     * @param asOfDate  the evaluation date (cycle date or today)
     * @return the overdue units, possibly empty
     */
    List<MeasurementReminderEmail.OverdueUnit> findOverdueUnits(UserEntity user, LocalDate asOfDate) {
        List<MeasurementReminderEmail.OverdueUnit> result = new ArrayList<>();
        // Deliberately a repository query instead of user.getUserMeasurementReminders():
        // the scheduler re-fetches the user inside its per-user transaction, and EclipseLink's
        // shared cache can serve that entity with a stale (initialized-empty) lazy collection.
        // Derived queries always execute SQL, so this read is correct regardless of cache state.
        for (UserMeasurementReminderEntity reminder : userMeasurementReminderRepository.findByUserId(user.getId())) {
            if (!reminder.isActive() || reminder.getUnitId() == null) {
                continue;
            }
            Optional<MeasurementEntity> lastMeasurement =
                    measurementRepository.findTopByUserAndUnitIdOrderByMeasuredOnDesc(user, reminder.getUnitId());

            boolean isOverdue;
            if (lastMeasurement.isEmpty()) {
                isOverdue = true; // never measured → always overdue
            } else {
                LocalDateTime lastMeasuredOn = lastMeasurement.get().getMeasuredOn();
                isOverdue = lastMeasuredOn != null
                        && lastMeasuredOn.toLocalDate().isBefore(asOfDate.minusDays(reminder.getPastdays()));
            }
            if (!isOverdue) {
                continue;
            }

            String unitSign = resolveUnitSign(reminder.getUnitId());
            if (lastMeasurement.isEmpty()) {
                result.add(new MeasurementReminderEmail.OverdueUnit(unitSign, null, null));
            } else {
                result.add(new MeasurementReminderEmail.OverdueUnit(
                        unitSign, lastMeasurement.get().getMeasuredValue(), lastMeasurement.get().getMeasuredOn()));
            }
        }
        return result;
    }

    private String resolveUnitSign(Integer unitId) {
        return unitRepository.findById(unitId)
                .map(UnitEntity::getName)
                .orElse("Unit " + unitId);
    }

}
