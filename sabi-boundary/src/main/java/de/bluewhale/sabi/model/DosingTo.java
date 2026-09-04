/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transfer object for a manual or automated aquarium dosing record.
 */
@Data
public class DosingTo implements Serializable {

    @Schema(description = "Internal PK, null when creating a new dosing record.")
    private Long id;

    @Schema(description = "ID of the aquarium this dosing belongs to. Set by the server from the URL.")
    private Long aquariumId;

    @NotNull
    @Schema(description = "Point in time at which dosing started or was recorded.", required = true)
    private LocalDateTime recordedOn;

    @NotNull
    @Schema(description = "Whether this is a manual addition or automated dosing.", required = true)
    private DosingType dosingType = DosingType.MANUAL_ADDITION;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Product, substance, or solution name.", required = true)
    private String productName;

    @NotNull
    @Positive
    @Digits(integer = 7, fraction = 3)
    @Schema(description = "Dosed amount; must be greater than zero.", required = true)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Unit of the amount, for example ml, g, tablets, or drops.", required = true)
    private String amountUnit;

    @Size(max = 80)
    @Schema(description = "Optional category, for example nutrient or calcium.")
    private String category;

    @Size(max = 40)
    @Schema(description = "Frequency of automated dosing, required only for automated dosing.")
    private String dosingInterval;

    @Size(max = 80)
    @Schema(description = "Optional dosing method, for example dosing pump.")
    private String dosingMethod;

    @Schema(description = "Optional solution or concentration description.")
    private String solutionDescription;

    @Schema(description = "Optional dosing note.")
    private String note;

    @Schema(description = "Optional end of an automated dosing schedule.")
    private LocalDateTime dosingEndOn;

    @Schema(description = "Timestamp when this record was created (server-set).")
    private LocalDateTime createdOn;

    @Schema(description = "Timestamp of last modification (server-set).")
    private LocalDateTime updatedOn;

    @Schema(description = "Optimistic lock version (server-set; send it back on PUT).")
    private long optlock;

    @JsonIgnore
    @AssertTrue(message = "Automated dosing requires a dosing interval; manual additions must not define an interval or end date.")
    public boolean isDosingScheduleValid() {
        if (dosingType == null || dosingType == DosingType.MANUAL_ADDITION) {
            return isBlank(dosingInterval) && dosingEndOn == null;
        }
        return !isBlank(dosingInterval);
    }

    @JsonIgnore
    @AssertTrue(message = "The automated dosing end date must be after the recorded date.")
    public boolean isDosingEndDateValid() {
        return dosingEndOn == null || recordedOn == null || dosingEndOn.isAfter(recordedOn);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
