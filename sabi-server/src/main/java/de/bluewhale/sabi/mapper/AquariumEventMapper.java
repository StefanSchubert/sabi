/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.persistence.model.AquariumEventEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper between {@link AquariumEventEntity} and {@link AquariumEventTo}.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface AquariumEventMapper {

    @Mappings({
        @Mapping(target = "id",           source = "id"),
        @Mapping(target = "aquariumId",   source = "aquariumId"),
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
        @Mapping(target = "createdOn",    source = "createdOn"),
        @Mapping(target = "updatedOn",    source = "lastmodOn"),   // lastmodOn -> updatedOn
        @Mapping(target = "optlock",      source = "optlock"),
    })
    AquariumEventTo mapEntityToTo(@NotNull AquariumEventEntity entity);

    List<AquariumEventTo> mapEntitiesToTos(@NotNull List<AquariumEventEntity> entities);

    @Mappings({
        @Mapping(target = "id",           source = "id"),
        @Mapping(target = "aquariumId",   source = "aquariumId"),
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
    })
    AquariumEventEntity mapToToEntity(@NotNull AquariumEventTo to);

    @Mappings({
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
        @Mapping(target = "id",           ignore = true),
        @Mapping(target = "aquariumId",   ignore = true),
    })
    void mergeToIntoEntity(@NotNull AquariumEventTo to,
                           @NotNull @MappingTarget AquariumEventEntity entity);
}

