/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * This one is used to submit measurement values through IoT devices
 * like this one https://github.com/StefanSchubert/aquarium_IoT#readme
 * The measurement type and tank will be derived from the API-Key.
 */
@Data
@Schema()
public class IoTMeasurementTo implements Serializable {

    @Schema(description= "Decimal value of the measurement. Unit is Celsius. Allowed values 18 to 35 °C . This already includes extreme" +
            "values. All other would'd make any sence.", required = true)
    @Min(18)
    @Max(35)
    private float measuredValueInCelsius;

    @Schema(description = "References the used unit and tank this measurement belongs to. The key can be obtained through SABIs tank menu", required = true)
    @NotNull
    private String apiKey;

}
