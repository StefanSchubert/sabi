/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.webclient.utils.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.annotation.SessionScope;

import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Locale;

/**
 * Container for mandatory things which we need to keep in a user session.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
public class UserSession implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String sabiBackendToken = "N/A";

    private String userName = "";

    private Locale locale;

    @Autowired
    private I18nUtil i18nUtil;

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Users locale
     *
     * @return
     */
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * After being successful authenticated against the sabi backend,
     * this token will be submitted via HTTP-Header whenever calling
     * sabis rest api backend.
     *
     * @return auth token, see {@link de.bluewhale.sabi.api.HttpHeader#AUTH_TOKEN}
     */
    public String getSabiBackendToken() {
        return sabiBackendToken;
    }

    /**
     * After being successful authenticated against the sabi backend,
     * this token will be submitted via HTTP-Header whenever calling
     * sabis rest api backend.
     *
     * @param sabiBackendToken
     */
    public void setSabiBackendToken(String sabiBackendToken) {
        this.sabiBackendToken = sabiBackendToken;
    }

    /**
     * To show in the app, which users session we are currently working on.
     *
     * @return Users Login name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set Users Name
     *
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Choosed Language of the User derived by browsers settings, if not explicit set before.
     *
     * @return
     */
    public String getLanguage() {

        if (locale == null) {
            Locale browsersLocale = LocaleContextHolder.getLocale();
            Locale supportedLocale = i18nUtil.getEnsuredSupportedLocale(browsersLocale.getLanguage());
            locale = supportedLocale;
        }

        return locale.getLanguage();
    }

    /**
     * Switch the display language of the current session, as long as it is supported by sabi, otherwise
     * english will be used as fallback.
     *
     * @param language
     */
    public void setLanguage(String language) {
        Locale requestedUserLocale;
        requestedUserLocale = i18nUtil.getEnsuredSupportedLocale(language);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(requestedUserLocale);
        locale = requestedUserLocale;
    }
}
