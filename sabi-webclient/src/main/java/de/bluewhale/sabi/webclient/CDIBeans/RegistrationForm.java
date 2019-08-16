/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import de.bluewhale.sabi.model.NewRegistrationTO;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 * Model for the Registration dialoge.
 * We extend the boundary model, to keep the boundary free of the Annotations required only for JSF,
 * while still being able to send this object over the wire to the backend API.
 *
 * @author Stefan Schubert
 */
@Named
@ViewScoped
public class RegistrationForm extends NewRegistrationTO {
}
