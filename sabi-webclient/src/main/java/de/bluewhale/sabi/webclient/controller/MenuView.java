/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import static de.bluewhale.sabi.webclient.utils.PageRegister.LOGOUT_PAGE;

/**
 * MenuActions
 *
 * @author Stefan Schubert
 */
@Named
@RequestScoped
@Slf4j
public class MenuView {

    // static String LOGOUT_PAGE = "/logout?faces-redirect=true";

    /**
     * Kills the sessions. The frontend session will be invalidated during rendering
     * of the logout page. This is necessary as it would otherwise collide
     * which WebSecurityConfig rule routing to expiredSessions.xhmtl if session is
     * no longer valid.
     * @return
     */
    public String logout() {
        log.info(" #### User logged out. ####");
        return "/"+LOGOUT_PAGE.getNavigationableAddress()+"?faces-redirect=true";
    }

}
