/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import javax.validation.constraints.Null;

/**
 * Provides common application services.
 */
public interface AppService {

    /**
     * Gets current valid Message of today. Used e.g. to announce maintenance windows to the user.
     * @param usersLanguage Locale.getLanguage
     * @return null if no such message exists, or "en" version if it exists, but the requested locale not.
     */
    @Null
    String fetchMotdFor(String usersLanguage);

}
