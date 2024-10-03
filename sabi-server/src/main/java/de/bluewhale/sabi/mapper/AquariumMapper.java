/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mappings for the aquarium modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface AquariumMapper {

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="sizeUnit", source="sizeUnit"),
            @Mapping(target ="waterType", source="waterType"),
            @Mapping(target ="size", source="size"),
            @Mapping(target ="description", source="description"),
            @Mapping(target ="userId", source="user.id"),
            @Mapping(target ="active", source="active"),
            @Mapping(target ="temperatureApiKey", source="temperatureApiKey"),
            @Mapping(target ="inceptionDate", source="inceptionDate"),
    })
    AquariumTo mapAquariumEntity2To(@NotNull final AquariumEntity pAquariumEntity);

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="sizeUnit", source="sizeUnit"),
            @Mapping(target ="waterType", source="waterType"),
            @Mapping(target ="size", source="size"),
            @Mapping(target ="description", source="description"),
            @Mapping(target ="active", source="active"),
            @Mapping(target ="temperatureApiKey", source="temperatureApiKey"),
            @Mapping(target ="inceptionDate", source="inceptionDate"),
            @Mapping(target ="user.id", source="userId"),
            @Mapping(target = "measurements", ignore = true),
            @Mapping(target = "plagueRecords", ignore = true)
    })
    AquariumEntity mapAquariumTo2Entity(@NotNull final AquariumTo pAquariumTo);

}
