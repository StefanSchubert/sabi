/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * TODO STS: Add Description here...
 *
 * @author Stefan Schubert
 */
@Named
@ApplicationScoped
public class ApplicationInfo {

    private String buildVersion = "v0.0.1 snapshot";

    public String getVersion() {
        return buildVersion;
    }
}
