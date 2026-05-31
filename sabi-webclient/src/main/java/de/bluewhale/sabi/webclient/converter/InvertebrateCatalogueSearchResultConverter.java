/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.InvertebrateCatalogueSearchResultTo;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * JSF Converter for InvertebrateCatalogueSearchResultTo.
 * Enables PrimeFaces p:autoComplete to round-trip complex objects via JSON.
 * Part of 006-invertebrate-tracking.
 */
@FacesConverter(value = "invertebrateCatalogueSearchResultConverter", managed = true)
public class InvertebrateCatalogueSearchResultConverter implements Converter<InvertebrateCatalogueSearchResultTo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public InvertebrateCatalogueSearchResultTo getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MAPPER.readValue(value, InvertebrateCatalogueSearchResultTo.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, InvertebrateCatalogueSearchResultTo value) {
        if (value == null) return "";
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "";
        }
    }
}
