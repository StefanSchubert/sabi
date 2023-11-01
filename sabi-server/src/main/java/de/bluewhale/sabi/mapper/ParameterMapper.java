/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.persistence.model.ParameterEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;

/**
 * Mappings for the Parameter Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface ParameterMapper {

    ParameterTo mapParameterEntity2To(@NotNull final ParameterEntity pParameterEntity);

    ParameterEntity mapParameterTo2Entity(ParameterTo pParameterTo);

}
