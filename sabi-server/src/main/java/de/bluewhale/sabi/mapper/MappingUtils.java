/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import org.mapstruct.Named;

/**
 * Used for required type conversions
 *
 * @author Stefan Schubert
 */
public class MappingUtils {

    @Named("DBIntegerToBoolean")
    public static Boolean dbIntegerToBoolean(int pInteger) {
        if (pInteger == 0) {
            return false;
        }
        if (pInteger == 1) {
            return true;
        }
        return null;
    }

    @Named("BooleanToDBInt")
    public static int booleanToDBInt(Boolean pBoolean) {
        if (Boolean.TRUE.equals(pBoolean)) {
            return 1;
        }
        return 0;
    }

}
