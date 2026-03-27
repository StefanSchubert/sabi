/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;

/**
 * Backing bean for OIDC error handling on the login page.
 * Reads the {@code ?error=} query parameter and provides a localized FacesMessage.
 *
 * Supported error codes:
 *   oidc         – invalid or expired ID token
 *   oidc_locked  – account is locked
 *   server       – backend server error
 *   cancelled    – user cancelled the OIDC flow
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class OidcErrorView implements Serializable {

    @Getter
    private String errorCode;

    @PostConstruct
    public void init() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) return;

        errorCode = context.getExternalContext()
                .getRequestParameterMap()
                .getOrDefault("error", "");

        if (!errorCode.isBlank()) {
            String msgKey = switch (errorCode) {
                case "oidc", "oidc_locked" -> "login.oidc.error.invalid_token";
                case "server"             -> "login.oidc.error.server";
                case "cancelled"          -> "login.oidc.error.cancelled";
                default                   -> null;
            };

            if (msgKey != null) {
                // Resolve via JSF resource bundle (already loaded as #{msg} in the view)
                String msgText = context.getApplication()
                        .getResourceBundle(context, "msg")
                        .getString(msgKey);
                context.addMessage("login-form",
                        new FacesMessage(FacesMessage.SEVERITY_WARN, msgText, null));
                log.warn("OIDC_ERROR_DISPLAYED code={}", errorCode);
            }
        }
    }
}

