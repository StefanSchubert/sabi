/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import jakarta.faces.application.NavigationHandler;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;

/**
 * Backing bean für die OIDC First-Login T&C Zustimmungsseite (sabi-150).
 * Wird aufgerufen wenn ein Nutzer sich erstmalig per Google-OIDC anmeldet
 * und den Nutzungsbedingungen und der DSGVO zustimmen muss.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class OidcWelcomeView implements Serializable {

    @Autowired
    private UserSession userSession;

    /**
     * Nutzer hat den Nutzungsbedingungen zugestimmt → weiter zum Userportal.
     * Das oidcFirstLogin-Flag wird zurückgesetzt.
     */
    public String acceptTerms() {
        userSession.setOidcFirstLogin(false);
        log.info("OIDC_TERMS_ACCEPTED username={}", userSession.getUserName());

        FacesContext facesContext = FacesContext.getCurrentInstance();
        NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
        navigationHandler.handleNavigation(facesContext, null, "/secured/userportal.xhtml?faces-redirect=true");
        return null;
    }

    /**
     * Nutzer hat abgelehnt → Session beenden und zurück zur Login-Seite.
     * Der provisioned Account bleibt bestehen (Backend-seitig), da Google-OIDC
     * beim nächsten Versuch erneut provisionen oder verknüpfen würde.
     */
    public String declineTerms() {
        log.info("OIDC_TERMS_DECLINED username={}", userSession.getUserName());
        userSession.endSession();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
        navigationHandler.handleNavigation(facesContext, null, "/login.xhtml?faces-redirect=true");
        return null;
    }
}

