/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.persistence.model.DosingEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper between dosing entities and transfer objects.
 */
@Mapper(componentModel = "spring")
public interface DosingMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "aquariumId", source = "aquariumId")
    @Mapping(target = "recordedOn", source = "recordedOn")
    @Mapping(target = "dosingType", source = "dosingType")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "amountUnit", source = "amountUnit")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "dosingInterval", source = "dosingInterval")
    @Mapping(target = "dosingMethod", source = "dosingMethod")
    @Mapping(target = "solutionDescription", source = "solutionDescription")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "dosingEndOn", source = "dosingEndOn")
    @Mapping(target = "createdOn", source = "createdOn")
    @Mapping(target = "updatedOn", source = "lastmodOn")
    @Mapping(target = "optlock", source = "optlock")
    DosingTo mapEntityToTo(@NotNull DosingEntity entity);

    List<DosingTo> mapEntitiesToTos(@NotNull List<DosingEntity> entities);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "recordedOn", source = "recordedOn")
    @Mapping(target = "dosingType", source = "dosingType")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "amountUnit", source = "amountUnit")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "dosingInterval", source = "dosingInterval")
    @Mapping(target = "dosingMethod", source = "dosingMethod")
    @Mapping(target = "solutionDescription", source = "solutionDescription")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "dosingEndOn", source = "dosingEndOn")
    DosingEntity mapToToEntity(@NotNull DosingTo to);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "recordedOn", source = "recordedOn")
    @Mapping(target = "dosingType", source = "dosingType")
    @Mapping(target = "productName", source = "productName")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "amountUnit", source = "amountUnit")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "dosingInterval", source = "dosingInterval")
    @Mapping(target = "dosingMethod", source = "dosingMethod")
    @Mapping(target = "solutionDescription", source = "solutionDescription")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "dosingEndOn", source = "dosingEndOn")
    void mergeToIntoEntity(@NotNull DosingTo to, @NotNull @MappingTarget DosingEntity entity);
}
