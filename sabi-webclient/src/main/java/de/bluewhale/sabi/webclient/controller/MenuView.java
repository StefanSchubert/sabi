/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.webclient.utils.MessageUtil;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * MenuActions
 *
 * @author Stefan Schubert
 */
@Named
@RequestScoped
@Slf4j
public class MenuView {

    public void logout() {
        // FIXME STS (13.04.20): Won't be called and no logging output with logback here
        log.info("User logged out.");
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().invalidateSession();
    }

    public void sendReport() {
        // TODO STS (13.04.20): Implement me.
        MessageUtil.info("common","Report will be generated and sent to your email shortly.");
    }

}
