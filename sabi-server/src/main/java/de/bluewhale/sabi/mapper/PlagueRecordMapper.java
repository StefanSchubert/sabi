/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.persistence.model.PlagueRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mappings for the PlagueRecord Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface PlagueRecordMapper {

    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="plagueId", source="plagueId"),
            @Mapping(target ="plagueStatusId", source="observedPlagueStatus"),
            @Mapping(target ="aquariumId", source="aquarium.id"),
            @Mapping(target ="observedOn", source="observedOn"),
            @Mapping(target ="plagueIntervallId", source="plagueIntervallId"),
    })
    PlagueRecordTo mapPlagueRecordEntity2To(PlagueRecordEntity pPlagueRecordEntity);

    @Mappings({
            @Mapping(target ="plagueId", source="plagueId"),
            @Mapping(source ="plagueStatusId", target="observedPlagueStatus"),
            @Mapping(target ="observedOn", source="observedOn"),
            @Mapping(target ="plagueIntervallId", source="plagueIntervallId"),
    })
    PlagueRecordEntity mapPlagueRecordTo2EntityWithoutPrimaryKeyAndAquarium(PlagueRecordTo pPlagueRecordTo);

}
