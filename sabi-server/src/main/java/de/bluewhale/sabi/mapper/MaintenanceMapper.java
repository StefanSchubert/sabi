/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceEventType;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.persistence.model.MaintenanceEventEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper between maintenance entities and transfer objects.
 */
@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "aquariumId", source = "aquariumId")
    @Mapping(target = "maintenanceType", source = "maintenanceType")
    @Mapping(target = "changedOn", source = "changedOn")
    @Mapping(target = "percentage", source = "percentage")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "createdOn", source = "createdOn")
    @Mapping(target = "updatedOn", source = "lastmodOn")
    @Mapping(target = "optlock", source = "optlock")
    MaintenanceEventTo mapEntityToTo(@NotNull MaintenanceEventEntity entity);

    List<MaintenanceEventTo> mapEntitiesToTos(@NotNull List<MaintenanceEventEntity> entities);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "maintenanceType", source = "maintenanceType")
    @Mapping(target = "changedOn", source = "changedOn")
    @Mapping(target = "percentage", source = "percentage")
    @Mapping(target = "note", source = "note")
    MaintenanceEventEntity mapToToEntity(@NotNull MaintenanceEventTo to);

    /**
     * Merges a PUT payload into an existing event. The maintenance type is intentionally
     * NOT merged: the type of a recorded event is fixed at creation time.
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "changedOn", source = "changedOn")
    @Mapping(target = "percentage", source = "percentage")
    @Mapping(target = "note", source = "note")
    void mergeToIntoEntity(@NotNull MaintenanceEventTo to, @NotNull @MappingTarget MaintenanceEventEntity entity);

    default MaintenanceEventType stringToMaintenanceType(String value) {
        return value == null ? null : MaintenanceEventType.fromString(value);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "regularIntervalDays", source = "regularIntervalDays")
    @Mapping(target = "regularPercentage", source = "regularPercentage")
    @Mapping(target = "carbonEnabled", source = "carbonEnabled")
    @Mapping(target = "carbonMode", source = "carbonMode")
    @Mapping(target = "carbonReplacementIntervalDays", source = "carbonReplacementIntervalDays")
    MaintenanceProfileTo mapProfileEntityToTo(@NotNull TankMaintenanceProfileEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "regularIntervalDays", source = "intervalDays")
    @Mapping(target = "regularPercentage", source = "percentage")
    void mergeIntervalIntoEntity(@NotNull WaterChangeIntervalTo to,
                                  @NotNull @MappingTarget TankMaintenanceProfileEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "carbonEnabled", source = "enabled")
    @Mapping(target = "carbonMode", source = "mode")
    @Mapping(target = "carbonReplacementIntervalDays", source = "replacementIntervalDays")
    void mergeCarbonIntoEntity(@NotNull ActivatedCarbonTo to,
                                @NotNull @MappingTarget TankMaintenanceProfileEntity entity);
}
