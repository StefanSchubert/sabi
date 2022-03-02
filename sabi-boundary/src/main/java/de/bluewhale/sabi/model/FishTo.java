/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Transportobject describing a fish
 *
 * @author Stefan Schubert
 */
public class FishTo {
// ------------------------------ FIELDS ------------------------------

    private Long id;
    private Long aquariumId;
    private Long fishCatalogueId;
    private LocalDate addedOn;
    private LocalDate exodusOn;
    private String nickname;
    private String observedBehavior;

// --------------------- GETTER / SETTER METHODS ---------------------

    @Schema(name = "Telling when the fish was added to the tank.", required = true)
    public LocalDate getAddedOn() {
        return this.addedOn;
    }

    public void setAddedOn(LocalDate addedOn) {
        this.addedOn = addedOn;
    }

    public Long getAquariumId() {
        return this.aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @Schema(name = "Telling when the fish died. By what reason ever.", required = false)
    public LocalDate getExodusOn() {
        return this.exodusOn;
    }

    public void setExodusOn(LocalDate exodusOn) {
        this.exodusOn = exodusOn;
    }

    @Schema(name = "Loose coupled reference to the fish catalogue which identifies the inhabitant within the community.", required = true)
    public Long getFishCatalogueId() {
        return this.fishCatalogueId;
    }

    public void setFishCatalogueId(Long fishCatalogueId) {
        this.fishCatalogueId = fishCatalogueId;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Schema(name = "For owners sake. Especially thought for being able to distinguish them when you have more of a kind.", required = true)
    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Schema(name = "Might be used by the owner to describe the observed behavior, so he or she might comapre it against public descriptions.", required = false)
    public String getObservedBehavior() {
        return this.observedBehavior;
    }

    public void setObservedBehavior(String observedBehavior) {
        this.observedBehavior = observedBehavior;
    }
}
