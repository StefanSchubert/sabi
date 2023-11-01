/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.persistence.model.UnitEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * Mappings for the Unit Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface UnitMapper {

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="unitSign", source="name"),
            @Mapping(target ="description", source="description"),
    })
    UnitTo mapUnitEntity2To(@NotNull final UnitEntity pUnitEntity);


    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="name", source="unitSign"),
            @Mapping(target ="description", source="description"),
    })
    UnitEntity mapUnitToEntity(@NotNull final UnitTo pUnitTo);

    List<UnitTo> mapUnitEntities2TOs(@NotNull List<UnitEntity> pUnitEntities);

}
