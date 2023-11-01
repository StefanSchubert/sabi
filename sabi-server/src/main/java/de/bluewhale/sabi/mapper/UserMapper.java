/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mappings for the User Modell
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface UserMapper {

    /**
     * <b>NOTICE:</b> Password won't be mapped here because of the encryption layer.
     * so you need to take care of it afterward.
     */
    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="email", source="email"),
            @Mapping(target ="username", source="username"),
            @Mapping(target ="validated", source="validated"),
            @Mapping(target ="validateToken", source="validationToken"),
            @Mapping(target ="language", source="language"),
            @Mapping(target ="country", source="country"),
    })
    UserEntity mapUserTo2Entity(@NotNull final UserTo pUserTo);


    /**
     * <b>NOTICE:</b> Password won't be mapped here because of the encryption layer.
     * so you need to take care of it afterward.
     */
    @Mappings({
            @Mapping(target ="id", source="id"),
            @Mapping(target ="email", source="email"),
            @Mapping(target ="username", source="username"),
            @Mapping(target ="validated", source="validated"),
            @Mapping(target ="validationToken", source="validateToken"),
            @Mapping(target ="language", source="language"),
            @Mapping(target ="country", source="country"),
    })
    UserTo mapUserEntity2To(@NotNull final UserEntity pUserEntity);

}
