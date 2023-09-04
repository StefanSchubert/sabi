/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import org.mapstruct.Named;

import java.sql.Timestamp;
import java.time.LocalDate;

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

    @Named("LocalDateToTimestamp")
    public static Timestamp localDateToTimestamp(LocalDate pLocalDate) {
        if (pLocalDate != null) {
            return Timestamp.valueOf(pLocalDate.atStartOfDay());
        } else {
            return null;
        }
    }

    @Named("TimestampToLocalDate")
    public static LocalDate timestampToLocalDate(Timestamp pTimestamp) {
        if (pTimestamp != null) {
            return pTimestamp.toLocalDateTime().toLocalDate();
        } else {
            return null;
        }
    }

}
