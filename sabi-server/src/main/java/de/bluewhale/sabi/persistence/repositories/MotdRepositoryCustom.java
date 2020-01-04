/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.MotdEntity;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface MotdRepositoryCustom {

    /**
     * Returns the current Motd.
     * Notice we will return only one valid record.
     *
     * @return null if no record can be found.
     */
    MotdEntity findValidMotd();

}
