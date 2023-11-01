/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.persistence.model.FishEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mappings for the fish modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface FishMapper {

    @Mappings({
            @Mapping(target ="addedOn", source="addedOn", qualifiedByName = "LocalDateToTimestamp"),
            @Mapping(target ="exodusOn", source="exodusOn", qualifiedByName = "LocalDateToTimestamp"),
            @Mapping(target ="nickname", source="nickname"),
            @Mapping(target ="observedBehavior", source="observedBehavior"),
            @Mapping(target ="aquariumId", source="aquariumId"),
            @Mapping(target ="fishCatalogueId", source="fishCatalogueId")
    })
    FishEntity mapFishTo2Entity(@NotNull final FishTo pFishTo);

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="addedOn", source="addedOn", qualifiedByName = "TimestampToLocalDate"),
            @Mapping(target ="exodusOn", source="exodusOn", qualifiedByName = "TimestampToLocalDate"),
            @Mapping(target ="nickname", source="nickname"),
            @Mapping(target ="observedBehavior", source="observedBehavior"),
            @Mapping(target ="aquariumId", source="aquariumId"),
            @Mapping(target ="fishCatalogueId", source="fishCatalogueId")
    })
    FishTo mapFishEntity2To(@NotNull final FishEntity pFishEntity);

}
