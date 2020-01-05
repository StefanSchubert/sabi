/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.persistence.model.LocalizedMotdEntity;
import de.bluewhale.sabi.persistence.model.MotdEntity;
import de.bluewhale.sabi.persistence.repositories.MotdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides common application services.
 */
@Service
public class AppServiceImpl implements AppService {

    static final String FALLBACK_LANGUAGE = "en";

    @Autowired
    MotdRepository motdRepository;

    @Override
    public String fetchMotdFor(String usersLanguage) {

        String result = null;
        String enFallback = null;
        MotdEntity validMotd = motdRepository.findValidMotd();

        if (validMotd != null) {
            List<LocalizedMotdEntity> localizedMotdEntities = validMotd.getLocalizedMotdEntities();
            for (LocalizedMotdEntity localizedMotdEntity : localizedMotdEntities) {
                if (localizedMotdEntity.getLanguage().equalsIgnoreCase(FALLBACK_LANGUAGE)) {
                    enFallback = localizedMotdEntity.getText();
                    if (usersLanguage.equalsIgnoreCase(FALLBACK_LANGUAGE)) break;
                }
                if (localizedMotdEntity.getLanguage().equalsIgnoreCase(usersLanguage)) {
                    result = localizedMotdEntity.getText();
                    break;
                }
            }
            return (result == null ? enFallback : result);
        }
        return result;
    }

}
