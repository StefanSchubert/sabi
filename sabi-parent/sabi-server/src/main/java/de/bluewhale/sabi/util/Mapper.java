/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.util;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishEntity;

import java.sql.Timestamp;

/**
 * Mapping Util Functions.
 * Since we have very few attributes per class, I decided to spare the libs for bean mappings like dozer.
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
    public static void mapAquariumEntity2To(final AquariumEntity pAquariumEntity, final AquariumTo pAquariumTo) {
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
    public static void mapAquariumTo2Entity(final AquariumTo pAquariumTo, final AquariumEntity pAquariumEntity) {
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
    public static void mapFishTo2Entity(final FishTo pFishTo, final FishEntity pFishEntity) {
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
    public static void mapFishEntity2To(final FishEntity pFishEntity, final FishTo pFishTo) {
        pFishTo.setId(pFishEntity.getId());
        pFishTo.setAddedOn(pFishEntity.getAddedOn().toLocalDateTime().toLocalDate());
        pFishTo.setExodusOn(pFishEntity.getExodusOn() == null ? null : pFishEntity.getExodusOn().toLocalDateTime().toLocalDate());
        pFishTo.setNickname(pFishEntity.getNickname());
        pFishTo.setObservedBehavior(pFishEntity.getObservedBehavior());
        pFishTo.setAquariumId(pFishEntity.getAquariumId());
        pFishTo.setFishCatalogueId(pFishEntity.getFishCatalogueId());
    }

}
