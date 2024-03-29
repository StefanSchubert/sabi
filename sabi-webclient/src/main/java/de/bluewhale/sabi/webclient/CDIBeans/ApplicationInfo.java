/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.Locale;

/**
 * Container for application specific static information.
 *
 * @author Stefan Schubert
 */
@Named
@ApplicationScoped
@Slf4j
public class ApplicationInfo implements Serializable {

    @Autowired
    BuildProperties buildProperties;

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    public static String buildVersion = "N/A";

    private RestTemplate restTemplate = new RestTemplate();

    public String getMotd() {
        // This is a little hack as using the viewAction from Metadata doesn't seem to work.
        fetchMotd();
        return "";
    }

    @PostConstruct
    public void lazyInit() {
        // Lazy Initialiser
        buildVersion = buildProperties.getVersion();
    }

    public String getVersion() {
        return buildVersion;
    }

    public void fetchCookieAnnouncement() {
        String cookieHint = MessageUtil.getFromMessageProperties("common.cookie.t", LocaleContextHolder.getLocale());
        String additionalHint = MessageUtil.getFromMessageProperties("common.additional.info.t", LocaleContextHolder.getLocale());

        MessageUtil.info("common", cookieHint);
        MessageUtil.info("additionalHint", additionalHint);

    }

    /**
     * Retrieves the Number of project participants.
     * @return Number as String
     */
    public String getNumberOfParticipants() {
        String numberOfParticipants = "N/A";
        String infoURL = sabiBackendUrl + Endpoint.PARTICIPANT_STATS;
        try {
            // todo only update max. every hour
            numberOfParticipants = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of participants stats");
            };
        } catch (RestClientException e) {
            // Default value will be displayed - which is ok. Stay resilient.
            log.error("Participant-Stats Restcall failure. {}",e.getMessage());
        }
        return numberOfParticipants;
    }

    /**
     * Retrieves the Number of registered tanks.
     * @return Number as String
     */
    public String getNumberOfTanks() {
        String numberOfTanks = "N/A";
        String infoURL = sabiBackendUrl + Endpoint.TANK_STATS;
        try {
            // todo only update max. every hour
            numberOfTanks = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of tanks stats");
            };
        } catch (RestClientException e) {
            // Default value will be displayed - which is ok. Stay resilient.
            log.error("Tank-Stats Restcall failure. {}",e.getMessage());
        }
        return numberOfTanks;
    }

    /**
     * Retrieves the Number of overall measurements
     * @return Number as String
     */
    public String getNumberOfMeasurements() {
        String numberOfMeasurements = "N/A";
        String infoURL = sabiBackendUrl + Endpoint.MEASUREMENT_STATS;
        try {
            // todo only update max. every hour
            numberOfMeasurements = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of measurement stats");
            };
        } catch (RestClientException e) {
            // Default value will be displayed - which is ok. Stay resilient.
            log.error("Measurement-Stats Restcall failure. {}",e.getMessage());
        }
        return numberOfMeasurements;
    }

    /**
     * Retrieves the Number of overall plague Records
     * @return Number as String
     */
    public String getNumberOfPlagueObeservationRecords() {
        String numberOfPlagueRecords = "N/A";
        String infoURL = sabiBackendUrl + Endpoint.PLAGUE_STATS;
        try {
            // todo only update max. every hour
            numberOfPlagueRecords = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of plaguerecord stats");
            };
        } catch (RestClientException e) {
            // Default value will be displayed - which is ok. Stay resilient.
            log.error("Plaguerecord-Stats Restcall failure. {}",e.getMessage());
        }
        return numberOfPlagueRecords;
    }

    /**
     * Retrieve the current message of today if there is any.
     *
     * @return
     */
    public void fetchMotd() {

        final Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();
        String motdURI = sabiBackendUrl + "/api/app/motd/" + language;
        MotdTo motdTo;

        try {
            motdTo = restTemplate.getForObject(motdURI, MotdTo.class);
            if (log.isDebugEnabled()){
                log.debug("Called MOTD");
            };
            if (motdTo != null && Strings.isNotEmpty(motdTo.getModt())) {
                MessageUtil.info("motd", motdTo.getModt());
            }
        } catch (RestClientException e) {
            // Treat as non existing MOTD to be resilient here.
            log.error("MOTD Restcall failure. {}",e.getMessage());
        }
    }

}
