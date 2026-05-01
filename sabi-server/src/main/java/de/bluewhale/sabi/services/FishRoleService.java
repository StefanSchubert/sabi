/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.FishRoleTo;

import java.util.List;

/**
 * Service for retrieving localized fish role data.
 *
 * @author Stefan Schubert
 */
public interface FishRoleService {

    /**
     * Returns all fish roles with localized names and descriptions for the given language.
     * Falls back to English if the requested language is not available.
     *
     * @param languageCode ISO 639-1 language code (e.g. "de", "en")
     * @return list of all fish roles with localized content
     */
    List<FishRoleTo> getFishRoles(String languageCode);

}
