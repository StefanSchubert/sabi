/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.converter;

import de.bluewhale.sabi.model.InvertebrateEcologicalRole;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * JSF Converter for {@link InvertebrateEcologicalRole} enum.
 * Required for p:selectManyCheckbox binding to List&lt;InvertebrateEcologicalRole&gt;
 * so that JSF can convert each string value back to the enum constant.
 * Part of 006-invertebrate-tracking.
 */
@FacesConverter(value = "invertebrateEcologicalRoleConverter")
public class InvertebrateEcologicalRoleConverter implements Converter<InvertebrateEcologicalRole> {

    @Override
    public InvertebrateEcologicalRole getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return InvertebrateEcologicalRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, InvertebrateEcologicalRole value) {
        return value == null ? "" : value.name();
    }
}
