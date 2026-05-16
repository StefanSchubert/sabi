/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.CoralCatalogueSearchResultTo;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * JSF Converter for CoralCatalogueSearchResultTo.
 * Enables PrimeFaces p:autoComplete to round-trip complex objects via JSON.
 * Part of 005-coral-stock.
 */
@FacesConverter(value = "coralCatalogueSearchResultConverter", managed = true)
public class CoralCatalogueSearchResultConverter implements Converter<CoralCatalogueSearchResultTo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public CoralCatalogueSearchResultTo getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MAPPER.readValue(value, CoralCatalogueSearchResultTo.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, CoralCatalogueSearchResultTo value) {
        if (value == null) return "";
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "";
        }
    }
}

