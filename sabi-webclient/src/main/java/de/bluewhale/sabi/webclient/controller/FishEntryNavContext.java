/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.model.FishStockEntryTo;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped navigation context that carries the fish entry between
 * the fishStockView page and the standalone fishStockEntryPage.
 *
 * Pattern: FishStockView stores the entry here before navigating;
 * FishStockEntryView reads it in @PostConstruct.
 */
@Named
@SessionScope
@Getter
@Setter
public class FishEntryNavContext implements Serializable {

    /** Entry to edit, duplicate or add. Set before navigating to fishStockEntryPage. */
    private FishStockEntryTo entry;

    public void prepare(FishStockEntryTo e) {
        this.entry = e;
    }

    public void clear() {
        this.entry = null;
    }
}

