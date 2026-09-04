/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.DosingTo;

import java.io.Serializable;
import java.util.List;

/**
 * Gateway for standalone aquarium dosing records.
 */
public interface DosingService extends Serializable {

    List<DosingTo> listDosingsForTank(Long aquariumId, String token) throws BusinessException;

    DosingTo createDosing(Long aquariumId, DosingTo dosing, String token) throws BusinessException;

    DosingTo updateDosing(Long aquariumId, Long dosingId, DosingTo dosing, String token) throws BusinessException;

    void deleteDosing(Long aquariumId, Long dosingId, String token) throws BusinessException;
}
