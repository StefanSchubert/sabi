/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * Mappings for the Measurement Model
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface MeasurementMapper {

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="measuredOn", source="measuredOn"),
            @Mapping(target ="measuredValue", source="measuredValue"),
            @Mapping(target ="aquariumId", source="aquarium.id"),
            @Mapping(target ="unitId", source="unitId"),
    })
    MeasurementTo mapMeasurementEntity2To(@NotNull final MeasurementEntity pMeasurementEntity);

    List<MeasurementTo> mapMeasurementEntities2TOs(@NotNull List<MeasurementEntity> measurementsOfAquarium);

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="measuredOn", source="measuredOn"),
            @Mapping(target ="measuredValue", source="measuredValue"),
            @Mapping(target ="unitId", source="unitId"),
            @Mapping(target ="aquarium", ignore = true),
            @Mapping(target ="user", ignore = true),
    })
    MeasurementEntity mapMeasurementTo2EntityWithoutAquarium(@NotNull final MeasurementTo pMeasurementTo);

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="measuredOn", source="measuredOn"),
            @Mapping(target ="measuredValue", source="measuredValue"),
            @Mapping(target ="unitId", source="unitId"),
            @Mapping(target ="aquarium", ignore = true),
            @Mapping(target ="user", ignore = true),
    })
    void mergeMeasurementTo2EntityWithoutAquarium(@NotNull final MeasurementTo pMeasurementTo,
                                                   @NotNull @MappingTarget MeasurementEntity pMeasurementEntity);

}
