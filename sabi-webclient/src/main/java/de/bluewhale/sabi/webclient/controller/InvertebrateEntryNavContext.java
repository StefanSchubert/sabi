/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.model.InvertebrateStockEntryTo;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped navigation context that carries the invertebrate entry between
 * the invertebrateStockView page and the standalone invertebrateStockEntryPage.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Getter
@Setter
public class InvertebrateEntryNavContext implements Serializable {

    /** Entry to edit or add. Set before navigating to invertebrateStockEntryPage. */
    private InvertebrateStockEntryTo entry;

    /** The aquarium ID to restore context for request-scoped beans. */
    private Long selectedAquariumId;

    public void prepare(InvertebrateStockEntryTo e) {
        this.entry = e;
        if (e != null) {
            this.selectedAquariumId = e.getAquariumId();
        }
    }

    public void clear() {
        this.entry = null;
    }
}
