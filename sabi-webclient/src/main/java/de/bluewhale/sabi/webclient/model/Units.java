/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.model;

import de.bluewhale.sabi.model.SizeUnit;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.inject.Named;

/**
 * Used to access the Enums in select Menues
 *
 * @author Stefan Schubert
 */
@Named
@ApplicationScope
public class Units {

    public SizeUnit[] getSizeUnits() {
        return SizeUnit.values();
    }

}
