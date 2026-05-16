/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralCatalogueService;
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
 * JSF CDI-Bean controller for the coral catalogue proposal/edit form (US6).
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralCatalogueProposalView implements Serializable {

    @Autowired
    CoralCatalogueService coralCatalogueService;

    @Inject
    UserSession userSession;

    private CoralCatalogueEntryTo currentEntry = new CoralCatalogueEntryTo();
    private boolean duplicateWarning = false;
    private boolean editMode = false;

    /** Map lang-code → i18n entry; populated in @PostConstruct.
     *  Use #{coralCatalogueProposalView.i18nByLang['de']} in EL —
     *  map-bracket access works via MapELResolver (no CGLIB method-proxy issue). */
    private Map<String, CoralCatalogueI18nTo> i18nByLang = new LinkedHashMap<>();

    private static final List<String> LANGUAGE_CODES = Arrays.asList("de", "en", "es", "fr", "it");

    @PostConstruct
    public void init() {
        // Ensure i18n entries for all 5 languages and populate the map
        for (String lang : LANGUAGE_CODES) {
            CoralCatalogueI18nTo existing = currentEntry.getI18nEntries() == null ? null :
                    currentEntry.getI18nEntries().stream()
                            .filter(e -> lang.equals(e.getLanguageCode()))
                            .findFirst().orElse(null);
            if (existing == null) {
                existing = new CoralCatalogueI18nTo();
                existing.setLanguageCode(lang);
                currentEntry.getI18nEntries().add(existing);
            }
            i18nByLang.put(lang, existing);
        }
    }

    public CoralClassification[] getClassifications() {
        return CoralClassification.values();
    }

    public CoralCareLevel[] getCareLevels() {
        return CoralCareLevel.values();
    }

    public List<String> getLanguageCodes() {
        return LANGUAGE_CODES;
    }

    // getI18nForLang(String) removed: Spring CGLIB proxies don't support
    // parameterised EL method calls. Use i18nByLang map instead:
    // #{coralCatalogueProposalView.i18nByLang['de']}

    public String onSave() {
        String token = userSession.getSabiBackendToken();
        log.debug("onSave: token prefix='{}', editMode={}",
                token != null && token.length() > 10 ? token.substring(0, 10) + "..." : token,
                editMode);
        if (token == null || "N/A".equals(token) || !token.startsWith("Bearer ")) {
            log.error("onSave: invalid backend token='{}' — user needs to re-login", token);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
        try {
            if (editMode && currentEntry.getId() != null) {
                coralCatalogueService.updateEntry(currentEntry, token);
            } else {
                currentEntry.setProposalDate(LocalDate.now());
                coralCatalogueService.proposeEntry(currentEntry, token);
                MessageUtil.info(null, "coralcatalogue.propose.success", userSession.getLocale());
            }
            return "/secured/coralStockView?faces-redirect=true";
        } catch (BusinessException e) {
            log.error("Failed to save coral catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
    }
}

