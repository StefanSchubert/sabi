/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.MeasurementReminderTo;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

/**
 * Mappings for the MeasurementReminder Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface MeasurementReminderMapper {

    @Mappings({
            @Mapping(target ="userId", source="user.id"),
            @Mapping(target ="pastDays", source="pastdays"),
            @Mapping(target ="active", source="active"),
            @Mapping(target ="unitId", source="unitId"),
    })
    MeasurementReminderTo mapUserMeasurementReminderEntity2TO(@NotNull UserMeasurementReminderEntity pUserMeasurementReminderEntity);

    @Mappings({
            @Mapping(target ="pastdays", source="pastDays"),
            @Mapping(target ="active", source="active"),
            @Mapping(target ="unitId", source="unitId"),
    })
    UserMeasurementReminderEntity mapUserMeasurementReminderTO2EntityWithoutUser(@NotNull MeasurementReminderTo pMeasurementReminderTo);

    @Mappings({
            @Mapping(target ="pastdays", source="pastDays"),
            @Mapping(target ="active", source="active"),
            @Mapping(target ="unitId", source="unitId"),
    })
    void mapUserMeasurementReminderTO2EntityWithoutUser(@NotNull MeasurementReminderTo pMeasurementReminderTo,
                                                        @NotNull @MappingTarget UserMeasurementReminderEntity pUserMeasurementReminderEntity);


}
