/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.SupportedLocales;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.UserService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.annotation.RequestScope;

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


    public Boolean getHasMeasurementReminders() {
        /* TODO STS (06.11.22): Impl me */
        return false;
    }

    public String save() {
        if (selectedLocale != null) {
            // Already stored
            try {
                UserProfileTo userProfileTo = new UserProfileTo(selectedLocale.getLanguage(), selectedLocale.getCountry());
                userService.updateUsersProfile(userProfileTo, userSession.getSabiBackendToken());
                userSession.setLocale(selectedLocale);
                userSession.setLanguage(selectedLocale.getLanguage());
                LocaleContextHolder.setLocale(selectedLocale); // used by spring
                MessageUtil.info("profileupdate","userprofile.updateconfirmation.t",userSession.getLocale());
            } catch (BusinessException e) {
                log.error("Could not update users Profile. {}",e.getMessage());
                MessageUtil.warn("profileupdate","common.error.internal_server_problem.t",userSession.getLocale());
            }
        }
        return USER_PROFILE_VIEW_PAGE;
    }

}
