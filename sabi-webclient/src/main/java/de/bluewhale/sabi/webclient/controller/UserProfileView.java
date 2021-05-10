/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

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

    // View Modell
    private String greeting;



}
