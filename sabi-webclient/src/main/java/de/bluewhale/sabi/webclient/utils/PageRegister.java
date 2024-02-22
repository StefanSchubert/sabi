/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

// One place to resolve page names for navigational issues.
public enum PageRegister {

    LOGIN_PAGE("login"),
    LOGOUT_PAGE("logout"),
    PLAGUE_VIEW_PAGE("plagueView"),
    REGISTER_PAGE("register"),
    PREREGISTER_PAGE("preregistration"),
    REPORT_VIEW_PAGE("reportView"),
    TANK_EDITOR_PAGE("tankEditor"),
    TANK_VIEW_PAGE("tankView"),
    MEASUREMENT_VIEW_PAGE("measureView"),
    USER_PROFILE_VIEW_PAGE("userProfile"),
    PASSWORD_FORGOTTEN_PAGE ("pwreset");

    String address;

    PageRegister(String pageAddress) {
        this.address = pageAddress;
    }

    public String getNavigationableAddress() {
        return address;
    }
}
