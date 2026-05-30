/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateCatalogueService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSF CDI-Bean controller for the invertebrate catalogue proposal/edit form.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class InvertebrateCatalogueProposalView implements Serializable {

    @Autowired
    InvertebrateCatalogueService invertebrateCatalogueService;

    @Inject
    UserSession userSession;

    private InvertebrateCatalogueEntryTo currentEntry = new InvertebrateCatalogueEntryTo();
    private boolean duplicateWarning = false;
    private boolean editMode = false;

    /** Map lang-code → i18n entry; populated in @PostConstruct.
     *  Use #{invertebrateCatalogueProposalView.i18nByLang['de']} in EL. */
    private Map<String, InvertebrateCatalogueI18nTo> i18nByLang = new LinkedHashMap<>();

    private static final List<String> LANGUAGE_CODES = Arrays.asList("de", "en", "es", "fr", "it");

    @PostConstruct
    public void init() {
        for (String lang : LANGUAGE_CODES) {
            InvertebrateCatalogueI18nTo existing = currentEntry.getI18nEntries() == null ? null :
                    currentEntry.getI18nEntries().stream()
                            .filter(e -> lang.equals(e.getLanguageCode()))
                            .findFirst().orElse(null);
            if (existing == null) {
                existing = new InvertebrateCatalogueI18nTo();
                existing.setLanguageCode(lang);
                currentEntry.getI18nEntries().add(existing);
            }
            i18nByLang.put(lang, existing);
        }
    }

    public InvertebrateTaxonomicCategory[] getTaxonomicCategories() {
        return InvertebrateTaxonomicCategory.values();
    }

    public CoralCareLevel[] getCareLevels() {
        return CoralCareLevel.values();
    }

    public List<String> getLanguageCodes() {
        return LANGUAGE_CODES;
    }

    public String onSave() {
        String token = userSession.getSabiBackendToken();
        if (token == null || "N/A".equals(token) || !token.startsWith("Bearer ")) {
            log.error("onSave: invalid backend token — user needs to re-login");
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
        try {
            if (editMode && currentEntry.getId() != null) {
                invertebrateCatalogueService.updateEntry(currentEntry, token);
            } else {
                currentEntry.setProposalDate(LocalDate.now());
                invertebrateCatalogueService.proposeEntry(currentEntry, token);
                MessageUtil.info(null, "invertebratecatalogue.propose.success", userSession.getLocale());
            }
            return "/secured/tankEditor?faces-redirect=true";
        } catch (BusinessException e) {
            log.error("Failed to save invertebrate catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
    }
}
