/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.util;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

/**
 * Mapping Util Functions.
 * Since we have very few attributes per class, I decided to spare the libs for bean mappings like dozer.
 * Also To2Entity-Direction will ommit the primary key (for security reasons)
 *
 * @author Stefan Schubert
 */
public class Mapper {

    /**
     * Maps given Entity attributes into provided TO.
     *
     * @param pAquariumEntity
     * @param pAquariumTo
     */
    public static void mapAquariumEntity2To(@NotNull final AquariumEntity pAquariumEntity, @NotNull final AquariumTo pAquariumTo) {
        pAquariumTo.setId(pAquariumEntity.getId());
        pAquariumTo.setSizeUnit(pAquariumEntity.getSizeUnit());
        pAquariumTo.setSize(pAquariumEntity.getSize());
        pAquariumTo.setDescription(pAquariumEntity.getDescription());
        pAquariumTo.setUserId(pAquariumEntity.getUser().getId());
        pAquariumTo.setActive(pAquariumEntity.getActive());
    }


    /**
     * Mapping without user relationsship
     *
     * @param pAquariumTo
     * @param pAquariumEntity
     */
    public static void mapAquariumTo2Entity(@NotNull final AquariumTo pAquariumTo, @NotNull final AquariumEntity pAquariumEntity) {
        pAquariumEntity.setId(pAquariumTo.getId());
        pAquariumEntity.setSizeUnit(pAquariumTo.getSizeUnit());
        pAquariumEntity.setSize(pAquariumTo.getSize());
        pAquariumEntity.setDescription(pAquariumTo.getDescription());
        pAquariumEntity.setActive(pAquariumTo.getActive());
    }

    /**
     * Mapping without flyweight relationsships to Aquarium and Catalogue
     *
     * @param pFishTo
     * @param pFishEntity
     */
    public static void mapFishTo2Entity(@NotNull final FishTo pFishTo, @NotNull final FishEntity pFishEntity) {
        pFishEntity.setAddedOn(Timestamp.valueOf(pFishTo.getAddedOn().atStartOfDay()));
        if (pFishTo.getExodusOn() != null) {
            pFishEntity.setExodusOn(Timestamp.valueOf(pFishTo.getExodusOn().atStartOfDay()));
        } else {
            pFishEntity.setExodusOn(null);
        }
        pFishEntity.setNickname(pFishTo.getNickname());
        pFishEntity.setObservedBehavior(pFishTo.getObservedBehavior());
        pFishEntity.setAquariumId(pFishTo.getAquariumId());
        pFishEntity.setFishCatalogueId(pFishTo.getFishCatalogueId());
    }

    /**
     * Maps given Entity attributes into provided TO.
     *
     * @param pFishTo
     * @param pFishEntity
     */
    public static void mapFishEntity2To(@NotNull final FishEntity pFishEntity, @NotNull final FishTo pFishTo) {
        pFishTo.setId(pFishEntity.getId());
        pFishTo.setAddedOn(pFishEntity.getAddedOn().toLocalDateTime().toLocalDate());
        pFishTo.setExodusOn(pFishEntity.getExodusOn() == null ? null : pFishEntity.getExodusOn().toLocalDateTime().toLocalDate());
        pFishTo.setNickname(pFishEntity.getNickname());
        pFishTo.setObservedBehavior(pFishEntity.getObservedBehavior());
        pFishTo.setAquariumId(pFishEntity.getAquariumId());
        pFishTo.setFishCatalogueId(pFishEntity.getFishCatalogueId());
    }

    /**
     * Maps given Entity attributes into provided TO.
     *
     * @param pMeasurementEntity
     * @param pMeasurementTo
     */
    public static void mapMeasurementEntity2To(@NotNull final MeasurementEntity pMeasurementEntity, @NotNull final MeasurementTo pMeasurementTo) {
        pMeasurementTo.setId(pMeasurementEntity.getId());
        if (pMeasurementEntity.getAquarium() != null) {
            pMeasurementTo.setAquariumId(pMeasurementEntity.getAquarium().getId());

        } else {
            pMeasurementTo.setAquariumId(null);
        }
        pMeasurementTo.setMeasuredOn(pMeasurementEntity.getMeasuredOn());
        pMeasurementTo.setMeasuredValue(pMeasurementEntity.getMeasuredValue());
        pMeasurementTo.setUnitId(pMeasurementEntity.getUnitId());
    }


    /**
     * Maps given To attributes into provided Entity.
     *
     * @param pMeasurementEntity
     * @param pMeasurementTo
     */
    public static void mapMeasurementTo2EntityWithoutAquarium(@NotNull final MeasurementTo pMeasurementTo, @NotNull final MeasurementEntity pMeasurementEntity) {
        pMeasurementEntity.setMeasuredOn(pMeasurementTo.getMeasuredOn());
        pMeasurementEntity.setMeasuredValue(pMeasurementTo.getMeasuredValue());
        pMeasurementEntity.setUnitId(pMeasurementTo.getUnitId());
        pMeasurementEntity.setId(pMeasurementTo.getId());
    }

    /**
     * Maps given To attributes into provided Entity
     *
     * @param pUserTo
     * @param pUserEntity
     */
    public static void mapUserTo2Entity(@NotNull final UserTo pUserTo, @NotNull final UserEntity pUserEntity) {
        pUserEntity.setId(pUserTo.getId());
        pUserEntity.setEmail(pUserTo.getEmail());
        pUserEntity.setUsername(pUserTo.getUsername());
        // pUserEntity.setPassword(pUserTo.getPassword()); No won't do because of encryption layer
        pUserEntity.setValidated(pUserTo.isValidated());
        pUserEntity.setValidateToken(pUserTo.getValidationToken());
        pUserEntity.setLanguage(pUserTo.getLanguage());
        pUserEntity.setCountry(pUserTo.getCountry());
    }

    /**
     * Maps givven Entity attributes into provided To
     *
     * @param pUserEntity
     * @param pUserTo
     */
    public static void mapUserEntity2To(@NotNull final UserEntity pUserEntity, @NotNull final UserTo pUserTo) {
        pUserTo.setId(pUserEntity.getId());
        pUserTo.setEmail(pUserEntity.getEmail());
        pUserTo.setUsername(pUserEntity.getUsername());
        // pUserTo.setPassword(pUserEntity.getPassword()); No won't do because of encryption layer
        pUserTo.setValidated(pUserEntity.isValidated());
        pUserTo.setValidationToken(pUserEntity.getValidateToken());
        pUserTo.setLanguage(pUserEntity.getLanguage());
        pUserTo.setCountry(pUserEntity.getCountry());
    }


}
