/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.util;

import de.bluewhale.sabi.model.MeasurementReminderTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;

/**
 * Mapping Util Functions.
 * Since we have very few attributes per class and the targeted runtime environment of a pi at the beginning,
 * I decided to spare the libs for bean mappings like dozer or MapStruts, to gain a slim jar file.
 * Also To2Entity-Direction will ommit the primary key (for security reasons)
 *
 * @author Stefan Schubert
 */
public class Mapper {


    public static void mapUserMeasurementReminderEntity2TO(UserMeasurementReminderEntity source, MeasurementReminderTo target) {
        target.setUserId(source.getUser().getId());
        target.setPastDays(source.getPastdays());
        target.setActive(source.isActive());
        target.setUnitId(source.getUnitId());
    }

    public static void mapUserMeasurementReminderTO2Entity(MeasurementReminderTo source, UserMeasurementReminderEntity target, UserEntity user ) {
        target.setUser(user);
        target.setActive(source.isActive());
        target.setPastdays(source.getPastDays());
        target.setUnitId(source.getUnitId());
    }

}
