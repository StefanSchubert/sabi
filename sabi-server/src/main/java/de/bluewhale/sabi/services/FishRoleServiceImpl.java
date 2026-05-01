/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.FishRoleTo;
import de.bluewhale.sabi.persistence.model.FishRoleEntity;
import de.bluewhale.sabi.persistence.model.LocalizedFishRoleEntity;
import de.bluewhale.sabi.persistence.repositories.FishRoleRepository;
import de.bluewhale.sabi.persistence.repositories.LocalizedFishRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link FishRoleService}.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class FishRoleServiceImpl implements FishRoleService {

    @Autowired
    private FishRoleRepository fishRoleRepository;

    @Autowired
    private LocalizedFishRoleRepository localizedFishRoleRepository;

    @Override
    public List<FishRoleTo> getFishRoles(String languageCode) {
        List<FishRoleEntity> allRoles = fishRoleRepository.findAll();

        // Load localized entries for the requested language; fall back to "en" if needed
        List<LocalizedFishRoleEntity> localizedEntries = localizedFishRoleRepository.findByLanguageCode(languageCode);
        if (localizedEntries.isEmpty() && !"en".equals(languageCode)) {
            log.debug("No fish role translations for '{}', falling back to 'en'", languageCode);
            localizedEntries = localizedFishRoleRepository.findByLanguageCode("en");
        }

        // Build a map: roleId → localized entry for O(1) lookup
        Map<Integer, LocalizedFishRoleEntity> localizedByRoleId = localizedEntries.stream()
                .collect(Collectors.toMap(
                        LocalizedFishRoleEntity::getRoleId,
                        e -> e,
                        (a, b) -> {
                            log.warn("Duplicate localized_fish_role entry for roleId={}, lang={} — keeping first",
                                    a.getRoleId(), a.getLanguageCode());
                            return a;
                        }
                ));

        List<FishRoleTo> result = new ArrayList<>();
        for (FishRoleEntity role : allRoles) {
            FishRoleTo to = new FishRoleTo();
            to.setId(role.getId());
            to.setEnumKey(role.getEnumKey());
            LocalizedFishRoleEntity loc = localizedByRoleId.get(role.getId());
            if (loc != null) {
                to.setLocalizedName(loc.getName());
                to.setLocalizedDescription(loc.getDescription());
            } else {
                // Last resort: use the enum key as name
                to.setLocalizedName(role.getEnumKey());
            }
            result.add(to);
        }
        return result;
    }
}
