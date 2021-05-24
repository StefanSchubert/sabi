/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
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
        final Locale locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        String cookieHint = MessageUtil.getFromMessageProperties("common.cookie.t", locale);
        String additionalHint = MessageUtil.getFromMessageProperties("common.additional.info.t", locale);
        MessageUtil.info("additionalHint", additionalHint);
        MessageUtil.info("common", cookieHint);
    }

    /**
     * Retrieves the Number of project participants.
     * @return Number as String
     */
    public String getNumberOfParticipants() {
        String numberOfParticipants = "N/A";
        String infoURL = sabiBackendUrl + "/api/stats/participants";
        try {
            // todo only update max. every hour
            numberOfParticipants = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of participants stats");
            };
        } catch (RestClientException e) {
            // Treat as non existing MOTD to be resilient here.
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
        String infoURL = sabiBackendUrl + "/api/stats/tanks";
        try {
            // todo only update max. every hour
            numberOfTanks = restTemplate.getForObject(infoURL, String.class);
            if (log.isDebugEnabled()){
                log.debug("Called Number of tanks stats");
            };
        } catch (RestClientException e) {
            // Treat as non existing MOTD to be resilient here.
            log.error("Tank-Stats Restcall failure. {}",e.getMessage());
        }
        return numberOfTanks;
    }

    /**
     * Retrieve the current message of today if there is any.
     *
     * @return
     */
    public void fetchMotd() {

        final Locale locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        String language = locale.getLanguage();
        String motdURI = sabiBackendUrl + "/api/app/motd/" + language;
        MotdTo motdTo;

        try {
            motdTo = restTemplate.getForObject(motdURI, MotdTo.class);
            if (log.isDebugEnabled()){
                log.debug("Called MOTD");
            };
            if (motdTo != null && Strings.isNotEmpty(motdTo.getMotd())) {
                MessageUtil.info("motd", motdTo.getMotd());
            }
        } catch (RestClientException e) {
            // Treat as non existing MOTD to be resilient here.
            log.error("MOTD Restcall failure. {}",e.getMessage());
        }
    }

}
