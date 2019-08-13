/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Container for application specific static information.
 *
 * @author Stefan Schubert
 */
@Named
@ApplicationScoped
public class ApplicationInfo implements Serializable {

    // TODO maven buid version into meta-INF and lazy init this here as property
    private String buildVersion = "v0.0.1 snapshot";

    public String getVersion() {
        return buildVersion;
    }
}
