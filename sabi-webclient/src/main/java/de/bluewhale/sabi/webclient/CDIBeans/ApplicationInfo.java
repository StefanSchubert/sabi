/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.model.UnitTo;
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
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
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
        String msg = MessageUtil.getFromMessageProperties("common.cookie.t", locale);
        MessageUtil.info("common", msg);
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
            log.error("Participant-Stats Restcall failure. {}",e);
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
            log.error("Tank-Stats Restcall failure. {}",e);
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
            log.error("MOTD Restcall failure. {}",e);
        }
    }

    /**
     * Used to request the Unitsign, when all you have is a reference Id.
     *
     * @param unitId technical key of the Unit.
     * @return "N/A" if unitId is unknown
     */
    @NotNull
    public static String getUnitSignForId(Integer unitId, List<UnitTo> fromUnitList) {
        // TODO STS (13.04.21): Impromement: instead of providing the list of known units those should be fetched here
        // wee need to change the auth scope on the api for this
        String result = "N/A";
        if (unitId != null) {
            for (UnitTo unitTo : fromUnitList) {
                if (unitTo.getId().equals(unitId)) {
                    result = unitTo.getUnitSign();
                    break;
                }
            }
        }
        if (result.equals("N/A")) {
            log.warn("Could not determine the unit sign for unitID: {}", unitId);
        }
        return result;
    }

    /**
     * Used to request the TankName, when all you have is a reference Id.
     *
     * @param tankId technical key of the Tank.
     * @return "N/A" if tankId is unknown
     */
    @NotNull
    public static String getTankNameForId(Long tankId, List<AquariumTo> tanks) {
        String result = "N/A";
        if (tankId != null) {
            for (AquariumTo aquariumTo : tanks) {
                if (aquariumTo.getId().equals(tankId)) {
                    result = aquariumTo.getDescription();
                    break;
                }
            }
        }
        if (result.equals("N/A")) {
            log.warn("Could not determine the tankname for tankID: {}", tankId);
        }
        return result;
    }

}
