/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.PublicReportService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.apigateway.UserService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDate;
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

    @Autowired
    TankService tankService;

    @Autowired
    PublicReportService publicReportService;

    @Value("${sabi.webclient.public.base-url:https://sabi-project.net}")
    private String publicBaseUrl;

    public List<SupportedLocales> supportedLocales;

    public List<MeasurementReminderTo> measurementReminderTos;

    public List<UnitTo> knownMeasurementUnits;

    public Locale selectedLocale;
    private Integer selectedUnitId;

    private boolean hasTanks;
    private boolean darkModeActive;

    /** Active tanks for which the user may generate public report links. */
    private List<AquariumTo> activeTanks = new ArrayList<>();

    /**
     * Map: aquariumId → active public link (null if no link exists for that tank).
     * Populated in {@link #init()}.
     */
    private Map<Long, PublicReportLinkTo> reportLinks = new LinkedHashMap<>();

    /** The tank selected by the user in the link management dropdown. */
    private Long selectedTankIdForLink;

    /** Optional expiry for a newly generated link (ISO date-time string, blank = no expiry). */
    private String newLinkValidUntil;

    @PostConstruct
    public void init() {
        SupportedLocales[] values = SupportedLocales.values();
        supportedLocales = Arrays.asList(values);
        selectedLocale = userSession.getLocale();
        darkModeActive = userSession.isDarkModeEnabled();

        try {
            measurementReminderTos = measurementService.getMeasurementRemindersForUser(userSession.getSabiBackendToken(),selectedLocale.getLanguage());
            knownMeasurementUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken(), userSession.getLanguage());
        } catch (BusinessException e) {
            measurementReminderTos = new ArrayList<>();
            knownMeasurementUnits = new ArrayList<>();
            log.error("Could not fetch users Profile. {}", e.getMessage());
            MessageUtil.warn("profileupdate", "common.error.internal_server_problem.t", userSession.getLocale());
        }

        try {
            activeTanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            hasTanks = !activeTanks.isEmpty();
            // Load any existing report links
            for (AquariumTo tank : activeTanks) {
                try {
                    PublicReportLinkTo link = publicReportService.getLinkForTank(tank.getId(), userSession.getSabiBackendToken());
                    reportLinks.put(tank.getId(), link); // null means no link
                } catch (BusinessException e) {
                    log.warn("Could not fetch report link for tank {}: {}", tank.getId(), e.getMessage());
                    reportLinks.put(tank.getId(), null);
                }
            }
        } catch (BusinessException e) {
            hasTanks = false;
            log.error("Could not fetch users tanks for portal hint. {}", e.getMessage());
        }
    }

    public Boolean getHasMeasurementReminders() {
        return !measurementReminderTos.isEmpty();
    }

    public Boolean getHasTanks() {
        return hasTanks;
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
                userProfileTo.setDarkModeEnabled(darkModeActive);
                userService.updateUsersProfile(userProfileTo, userSession.getSabiBackendToken());
                userSession.setLocale(selectedLocale);
                userSession.setLanguage(selectedLocale.getLanguage());
                userSession.setDarkModeEnabled(darkModeActive);
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

    /**
     * JSF action method for the AI export button (ajax="false").
     * Streams the JSON export file to the browser as an attachment.
     * Reuses the existing {@code hasTanks} field — no separate check needed.
     */
    public void downloadReefData() {
        try {
            byte[] data = userService.downloadReefDataExport(userSession.getSabiBackendToken());

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();

            ec.responseReset();
            ec.setResponseContentType("application/octet-stream");
            String filename = "sabi-reef-data-" + LocalDate.now() + ".json";
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            ec.setResponseContentLength(data.length);

            OutputStream out = ec.getResponseOutputStream();
            out.write(data);
            out.flush();

            fc.responseComplete();
        } catch (BusinessException | IOException e) {
            log.error("Could not download reef data export. {}", e.getMessage());
            MessageUtil.warn("messages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    // ---- Public report link management ----

    /**
     * Returns the full public URL for the given share token.
     */
    public String getPublicReportUrl(String linkToken) {
        return publicBaseUrl + "/houseReefReport.xhtml?token=" + linkToken;
    }

    /**
     * Returns the existing link for a tank, or null if none.
     */
    public PublicReportLinkTo getReportLinkFor(Long aquariumId) {
        return reportLinks.get(aquariumId);
    }

    /**
     * Generates (or replaces) the public report link for the selected tank.
     */
    public String generateReportLink() {
        if (selectedTankIdForLink == null) {
            MessageUtil.warn("messages", "housereef.report.link.no_tank_selected.t", userSession.getLocale());
            return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
        }
        try {
            LocalDateTime validUntil = null;
            if (newLinkValidUntil != null && !newLinkValidUntil.isBlank()) {
                try {
                    validUntil = LocalDateTime.parse(newLinkValidUntil);
                } catch (Exception ex) {
                    log.warn("Could not parse validUntil '{}': {}", newLinkValidUntil, ex.getMessage());
                }
            }
            PublicReportLinkTo link = publicReportService.createOrReplaceLink(
                    selectedTankIdForLink, validUntil, userSession.getSabiBackendToken());
            reportLinks.put(selectedTankIdForLink, link);
            MessageUtil.info("messages", "housereef.report.link.generated.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not generate public report link for tank {}: {}", selectedTankIdForLink, e.getMessage());
            MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
        return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
    }

    /**
     * Deletes the public report link for the given tank.
     */
    public String deleteReportLink(Long aquariumId) {
        try {
            publicReportService.deleteLink(aquariumId, userSession.getSabiBackendToken());
            reportLinks.put(aquariumId, null);
            MessageUtil.info("messages", "housereef.report.link.deleted.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not delete public report link for tank {}: {}", aquariumId, e.getMessage());
            MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
        return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
    }
}


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

    @Autowired
    TankService tankService;

    public List<SupportedLocales> supportedLocales;

    public List<MeasurementReminderTo> measurementReminderTos;

    public List<UnitTo> knownMeasurementUnits;

    public Locale selectedLocale;
    private Integer selectedUnitId;

    private boolean hasTanks;
    private boolean darkModeActive;

    @PostConstruct
    public void init() {
        SupportedLocales[] values = SupportedLocales.values();
        supportedLocales = Arrays.asList(values);
        selectedLocale = userSession.getLocale();
        darkModeActive = userSession.isDarkModeEnabled();

        try {
            measurementReminderTos = measurementService.getMeasurementRemindersForUser(userSession.getSabiBackendToken(),selectedLocale.getLanguage());
            knownMeasurementUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken(), userSession.getLanguage());
        } catch (BusinessException e) {
            measurementReminderTos = new ArrayList<>();
            knownMeasurementUnits = new ArrayList<>();
            log.error("Could not fetch users Profile. {}", e.getMessage());
            MessageUtil.warn("profileupdate", "common.error.internal_server_problem.t", userSession.getLocale());
        }

        try {
            hasTanks = !tankService.getUsersTanks(userSession.getSabiBackendToken()).isEmpty();
        } catch (BusinessException e) {
            hasTanks = false;
            log.error("Could not fetch users tanks for portal hint. {}", e.getMessage());
        }
    }

    public Boolean getHasMeasurementReminders() {
        return !measurementReminderTos.isEmpty();
    }

    public Boolean getHasTanks() {
        return hasTanks;
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
                userProfileTo.setDarkModeEnabled(darkModeActive);
                userService.updateUsersProfile(userProfileTo, userSession.getSabiBackendToken());
                userSession.setLocale(selectedLocale);
                userSession.setLanguage(selectedLocale.getLanguage());
                userSession.setDarkModeEnabled(darkModeActive);
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

    /**
     * JSF action method for the AI export button (ajax="false").
     * Streams the JSON export file to the browser as an attachment.
     * Reuses the existing {@code hasTanks} field — no separate check needed.
     */
    public void downloadReefData() {
        try {
            byte[] data = userService.downloadReefDataExport(userSession.getSabiBackendToken());

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();

            ec.responseReset();
            ec.setResponseContentType("application/octet-stream");
            String filename = "sabi-reef-data-" + LocalDate.now() + ".json";
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            ec.setResponseContentLength(data.length);

            OutputStream out = ec.getResponseOutputStream();
            out.write(data);
            out.flush();

            fc.responseComplete();
        } catch (BusinessException | IOException e) {
            log.error("Could not download reef data export. {}", e.getMessage());
            MessageUtil.warn("messages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }
}
