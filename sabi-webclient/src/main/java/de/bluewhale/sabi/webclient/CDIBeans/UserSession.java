/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.model.SupportedLocales;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
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

    private String sabiBackendToken;

    private String userName = "";

    private Locale locale;

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Users locale
     *
     * @return
     */
    public Locale getLocale() {
        return locale;
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
     * Choosed Language of the User
     *
     * @return
     */
    public String getLanguage() {
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
        requestedUserLocale = getEnsuredSupportedLocale(language);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(requestedUserLocale);
        locale = requestedUserLocale;
    }

    /**
     * Gets the current locale from users browser and set it as default -
     * as long as it is supported by sabi, if not we choose english as fallback.
     */
    @PostConstruct
    private void init() {
        locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        Locale supportedLocale;
        supportedLocale = getEnsuredSupportedLocale(locale.getLanguage());
        FacesContext.getCurrentInstance().getViewRoot().setLocale(supportedLocale);
        locale = supportedLocale;
    }

    /**
     * Checks Backend for some Info message which should be displayed and set it as Faces Message Context.
     */
   @PostConstruct
   private void checkForSystemMessage(){
        // TODO STS (2019-08-18): Implement Backendfunction to retrieve some motd.
       MessageUtil.info("DEV Environment: WIP (Implement MOTD Function :-))");
   }

    /**
     * Sabi has only a few languages for which translation exists - this ensures that
     *
     * @param language
     * @return belonging requested URL or English as Fallback.
     */
    private Locale getEnsuredSupportedLocale(String language) {
        Locale requestedLocale = new Locale(language);
        Locale fallBackLocale = Locale.ENGLISH;
        boolean fallBack = true;
        for (SupportedLocales sabiLocale : SupportedLocales.values()) {
            if (sabiLocale.getLocale().getLanguage().equals(requestedLocale.getLanguage())) {
                fallBack = false;
                break;
            }
        }
        return (fallBack ? fallBackLocale : requestedLocale);
    }
}
