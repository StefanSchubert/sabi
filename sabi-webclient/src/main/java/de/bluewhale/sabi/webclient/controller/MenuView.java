/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

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

    static String LOGOUT_PAGE = "/logout?faces-redirect=true";

    public String logout() {
        log.info(" #### User logged out. ####");
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().invalidateSession();
        return LOGOUT_PAGE;
    }

}
