/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.util;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;

/**
 * Mapping Util Functions.
 * Since we have very few attributes per class, I decided to spare the libs for bean mappings like dozer.
 *
 * @author Stefan Schubert
 */
public class Mapper {

    /**
     * Maps given Entity attributes into provided TO.
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
     * @param pAquariumTo
     * @param pAquariumEntity
     */
    public static void mapAquariumTo2Entity(final AquariumTo pAquariumTo, final AquariumEntity pAquariumEntity) {
        pAquariumEntity.setSizeUnit(pAquariumTo.getSizeUnit());
        pAquariumEntity.setSize(pAquariumTo.getSize());
        pAquariumEntity.setDescription(pAquariumTo.getDescription());
        pAquariumEntity.setActive(pAquariumTo.getActive());
    }

}
