/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.model.CoralStockEntryTo;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped navigation context that carries the coral entry between
 * the coralStockView page and the standalone coralStockEntryPage.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Getter
@Setter
public class CoralEntryNavContext implements Serializable {

    /** Entry to edit or add. Set before navigating to coralStockEntryPage. */
    private CoralStockEntryTo entry;

    /** The aquarium ID to restore context for request-scoped beans. */
    private Long selectedAquariumId;

    public void prepare(CoralStockEntryTo e) {
        this.entry = e;
        if (e != null) {
            this.selectedAquariumId = e.getAquariumId();
        }
    }

    public void clear() {
        this.entry = null;
    }
}

