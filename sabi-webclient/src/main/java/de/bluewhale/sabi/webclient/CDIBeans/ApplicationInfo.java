/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
public class ApplicationInfo implements Serializable {

    static Logger logger = LoggerFactory.getLogger(ApplicationInfo.class);

    // TODO maven build version into meta-INF and lazy init this here as property
    public static String buildVersion = "v0.0.1 snapshot";
    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;
    private RestTemplate restTemplate = new RestTemplate();

    public String getMotd() {
        // This is a little hack as using the viewAction from Metadata doesn't seem to work.
        fetchMotd();
        return "";
    }

    public String getVersion() {
        return ApplicationInfo.buildVersion;
    }

    public void fetchCookieAnnouncement() {
        final Locale locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        String msg = MessageUtil.getFromMessageProperties("common.cookie.t", locale);
        MessageUtil.info("common", msg);
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
            if (motdTo != null && Strings.isNotEmpty(motdTo.getMotd())) {
                MessageUtil.info("motd", motdTo.getMotd());
            }
        } catch (RestClientException e) {
            // Treat as non existing MOTD to be resilient here.
            logger.error("MOTD Restcall failure.",e);
        }
    }

}
