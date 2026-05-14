/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumEventTo;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * API gateway interface for aquarium logbook events.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
public interface AquariumEventService extends Serializable {

    List<AquariumEventTo> listEventsForTank(Long aquariumId, String token) throws BusinessException;

    AquariumEventTo createEvent(Long aquariumId, AquariumEventTo event, String token) throws BusinessException;

    AquariumEventTo updateEvent(Long aquariumId, Long eventId, AquariumEventTo event, String token)
            throws BusinessException;

    void deleteEvent(Long aquariumId, Long eventId, String token) throws BusinessException;
}

