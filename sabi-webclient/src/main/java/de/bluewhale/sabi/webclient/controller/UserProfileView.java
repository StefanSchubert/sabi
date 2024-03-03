/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.MeasurementReminderTo;
import de.bluewhale.sabi.model.SupportedLocales;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.UserService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static de.bluewhale.sabi.webclient.utils.PageRegister.USER_PROFILE_VIEW_PAGE;

/**
 * Controller for the Report View as shown in reportView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Data
public class UserProfileView extends AbstractControllerTools implements Serializable {

    private static final int DEFAULT_MEASUREMENT_INTERVAL_IN_DAYS = 7;

    @Inject
    UserSession userSession;

    @Autowired
    UserService userService;

    @Autowired
    MeasurementService measurementService;

    public List<SupportedLocales> supportedLocales;

    public List<MeasurementReminderTo> measurementReminderTos;

    public List<UnitTo> knownMeasurementUnits;

    public Locale selectedLocale;
    private Integer selectedUnitId;

    @PostConstruct
    public void init() {
        SupportedLocales[] values = SupportedLocales.values();
        supportedLocales = Arrays.asList(values);
        selectedLocale = userSession.getLocale();

        try {
            measurementReminderTos = measurementService.getMeasurementRemindersForUser(userSession.getSabiBackendToken(),selectedLocale.getLanguage());
            knownMeasurementUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken(), userSession.getLanguage());
        } catch (BusinessException e) {
            measurementReminderTos = new ArrayList<>();
            knownMeasurementUnits = new ArrayList<>();
            log.error("Could not fetch users Profile. {}", e.getMessage());
            MessageUtil.warn("profileupdate", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public Boolean getHasMeasurementReminders() {
        return !measurementReminderTos.isEmpty();
    }

    /**
     * Used as Workaround to create dynamic images resources
     * @return
     */
    public String getFlagResource(Locale c) {
        return "images:icons8-flag-" + c.getLanguage() + "-48.png";
    }



    public String save() {
        if (selectedLocale != null) {
            // Already stored
            try {
                UserProfileTo userProfileTo = new UserProfileTo(selectedLocale.getLanguage(), selectedLocale.getCountry());
                userProfileTo.setMeasurementReminderTos(measurementReminderTos);
                userService.updateUsersProfile(userProfileTo, userSession.getSabiBackendToken());
                userSession.setLocale(selectedLocale);
                userSession.setLanguage(selectedLocale.getLanguage());
                LocaleContextHolder.setLocale(selectedLocale); // used by spring
                MessageUtil.info("profileupdate", "userprofile.updateconfirmation.t", userSession.getLocale());
            } catch (BusinessException e) {
                log.error("Could not update users Profile. {}", e.getMessage());
                MessageUtil.warn("profileupdate", "common.error.internal_server_problem.t", userSession.getLocale());
            }
        }
        return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
    }

    public String addReminderForMeasurementUnit() {
        if (selectedUnitId != null) {

            MeasurementReminderTo measurementReminderTo = new MeasurementReminderTo();
            measurementReminderTo.setUnitId(selectedUnitId);
            Optional<UnitTo> optionalUnitTo = knownMeasurementUnits.stream().filter(item -> item.getId().equals(selectedUnitId)).findFirst();
            if (optionalUnitTo.isPresent()) {
                measurementReminderTo.setUnitName(optionalUnitTo.get().getUnitSign());
            } else {
                measurementReminderTo.setUnitName("N/A?");
            }
            measurementReminderTo.setPastDays(DEFAULT_MEASUREMENT_INTERVAL_IN_DAYS);

            try {
                measurementService.addMeasurementReminder(measurementReminderTo, userSession.getSabiBackendToken());
                measurementReminderTos = measurementService.getMeasurementRemindersForUser(userSession.getSabiBackendToken(), userSession.getLanguage());
            } catch (BusinessException e) {
                log.error("Could not add new Measurement Reminder.", e);
                MessageUtil.warn("messages", "common.error.internal_server_problem.t", userSession.getLocale());
            }

        }
        return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
    }


    public Integer getSelectedUnitId() {
        return selectedUnitId;
    }

    public void setSelectedUnitId(Integer selectedUnitId) {
        this.selectedUnitId = selectedUnitId;
    }

    /**
     * Used to color the font output if the measuredate is overdue.
     * @param pLocalDateTime
     * @return false if next measure date is in the future, else true.
     */
    public Boolean isOverdueMeasureDate(LocalDateTime pLocalDateTime) {
        if (pLocalDateTime == null) {
            return false;
        } else {
            return (pLocalDateTime.toLocalDate().isBefore(LocalDateTime.now().toLocalDate()) ? true : false);
        }
    }
}
