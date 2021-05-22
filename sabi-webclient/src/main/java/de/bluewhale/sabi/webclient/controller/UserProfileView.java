/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.SupportedLocales;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.UserService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Report View as shown in reportView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Data
public class UserProfileView extends AbstractControllerTools implements Serializable {

    private static final String USER_PROFILE_VIEW_PAGE = "userProfile";

    @Inject
    UserSession userSession;

    @Autowired
    UserService userService;

    public List<SupportedLocales> supportedLocales;

    public Locale selectedLocale;

    @PostConstruct
    public void init() {
        SupportedLocales[] values = SupportedLocales.values();
        supportedLocales = Arrays.asList(values);

        selectedLocale = userSession.getLocale();
    }


    public String save() {
        if (selectedLocale != null) {
            // Already stored
            try {
                userService.switchUsersLocale(selectedLocale, userSession.getSabiBackendToken());
                userSession.setLocale(selectedLocale);
                userSession.setLanguage(selectedLocale.getLanguage());
                MessageUtil.info("messages","userprofile.updateconfirmation.t",userSession.getLocale());
            } catch (BusinessException e) {
                e.printStackTrace();
                MessageUtil.warn("messages","common.error.internal_server_problem.t",userSession.getLocale());
            }
        }
        return USER_PROFILE_VIEW_PAGE;
    }

}
