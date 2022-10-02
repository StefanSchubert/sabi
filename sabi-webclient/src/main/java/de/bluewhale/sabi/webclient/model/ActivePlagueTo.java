/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Used to display a active in PlagueCenter
 *
 * @author Stefan Schubert
 */
@Data
public class ActivePlagueTo {

    private String tankName;
    private String plageName;
    private LocalDateTime observedOn;
    private String currentStatus;

}
