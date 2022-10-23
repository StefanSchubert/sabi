/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.model;

import lombok.Data;

/**
 * Used to display a survived plage in PlagueCenter
 *
 * @author Stefan Schubert
 */
@Data
public class PastPlagueTo extends ReportedPlagueTo {

    /**
     * Duration in days
     */
    private Long duration;

}
