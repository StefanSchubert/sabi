/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Transport Objekt which references the fish with a catalogue, to support
 * a common fish base. This is the required common attribute. Without it
 * we would't be able to analyse treatments between the different tanks.
 *
 * @author Stefan Schubert
 */
public class FishCatalogueTo {
// ------------------------------ FIELDS ------------------------------

    private Long id;
    private String scientificName;
    private String description;
    private String meerwasserwikiUrl;

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "Short description of the fish (or localized name?), the detailed description relies in public wikis.", required = false)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ApiModelProperty(notes = "Link to a public wiki which describes the fish in detail.", required = false)
    public String getMeerwasserwikiUrl() {
        return this.meerwasserwikiUrl;
    }

    public void setMeerwasserwikiUrl(String meerwasserwikiUrl) {
        this.meerwasserwikiUrl = meerwasserwikiUrl;
    }

    @ApiModelProperty(notes = "Scienetific name of the fish, which might ease the fish lookup from the catalogue.", required = true)
    public String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }
}
