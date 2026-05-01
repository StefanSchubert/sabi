/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.DepartureReason;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.persistence.model.TankFishStockEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * MapStruct mapper for TankFishStockEntity <-> FishStockEntryTo.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface FishStockMapper {

    @Mappings({
            @Mapping(target = "user", ignore = true),
            @Mapping(target = "deletedAt", ignore = true),
            @Mapping(target = "addedOn", source = "addedOn"),
            @Mapping(target = "exodusOn", source = "exodusOn"),
            @Mapping(target = "departureReason", expression = "java(to.getDepartureReason() != null ? to.getDepartureReason().name() : null)"),
            @Mapping(target = "departureNote", ignore = true)
    })
    TankFishStockEntity mapTo2Entity(FishStockEntryTo to);

    @Mappings({
            @Mapping(target = "hasPhoto", ignore = true),
            @Mapping(target = "departureReason", expression = "java(entity.getDepartureReason() != null ? de.bluewhale.sabi.model.DepartureReason.valueOf(entity.getDepartureReason()) : null)"),
            @Mapping(target = "departureNote", source = "departureNote")
    })
    FishStockEntryTo mapEntity2To(TankFishStockEntity entity);

}
