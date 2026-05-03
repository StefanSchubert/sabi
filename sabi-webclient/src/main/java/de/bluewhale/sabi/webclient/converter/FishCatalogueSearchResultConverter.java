/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * JSF Converter for FishCatalogueSearchResultTo.
 * Enables PrimeFaces p:autoComplete to round-trip complex objects via JSON.
 */
@FacesConverter(value = "fishCatalogueSearchResultConverter", managed = true)
public class FishCatalogueSearchResultConverter implements Converter<FishCatalogueSearchResultTo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public FishCatalogueSearchResultTo getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MAPPER.readValue(value, FishCatalogueSearchResultTo.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, FishCatalogueSearchResultTo value) {
        if (value == null) return "";
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "";
        }
    }
}

