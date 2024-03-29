/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO of a users profile
 */
@Data
@NoArgsConstructor
public class UserProfileTo implements Serializable {

    @Schema(name="ISO-639-1 language code - used for i18n in communication. Must be set together with country. Defaults to 'en'",required = true)
    private String language ="en"; // default
    @Schema(description =  "ISO-3166-1 alpha-2 country code - used for i18n in communication. Default 'US'", required = true)
    private String country = "US"; // default

    @Schema(description = "Contains 'remind me to measure something' records.", required = false)
    private List<MeasurementReminderTo> measurementReminderTos = new ArrayList<>();

    public UserProfileTo(String pLanguage, String pCountry) {
        this.language = pLanguage;
        this.country = pCountry;
    }

}
