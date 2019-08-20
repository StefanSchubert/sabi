/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.webclient.utils.MessageUtil;

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

    // TODO maven buid version into meta-INF and lazy init this here as property
    private String buildVersion = "v0.0.1 snapshot";

    public String getVersion() {
        return buildVersion;
    }

    public void fetchCookieAnnouncement(){
        final Locale locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        String msg = MessageUtil.getFromMessageProperties("common.cookie.t", locale);
        MessageUtil.info("common",msg);
    }
}
