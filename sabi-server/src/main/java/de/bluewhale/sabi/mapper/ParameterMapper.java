/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.persistence.model.ParameterEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

/**
 * Mappings for the Parameter Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface ParameterMapper {

    @Mappings({
            @org.mapstruct.Mapping(target = "id", source = "id"),
            @org.mapstruct.Mapping(target = "belongingUnitId", source = "belongingUnitId"),
            @org.mapstruct.Mapping(target = "minThreshold", source = "minThreshold"),
            @org.mapstruct.Mapping(target = "maxThreshold", source = "maxThreshold"),
            @org.mapstruct.Mapping(target = "description", ignore = true)
    })
    ParameterTo mapParameterEntity2To(@NotNull final ParameterEntity pParameterEntity);

    @Mappings({
            @org.mapstruct.Mapping(target = "id", source = "id"),
            @org.mapstruct.Mapping(target = "belongingUnitId", source = "belongingUnitId"),
            @org.mapstruct.Mapping(target = "minThreshold", source = "minThreshold"),
            @org.mapstruct.Mapping(target = "maxThreshold", source = "maxThreshold"),
            @org.mapstruct.Mapping(target = "localizedParameterEntities", ignore = true)
    })
    ParameterEntity mapParameterTo2Entity(ParameterTo pParameterTo);

}
