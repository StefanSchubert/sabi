/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls Sabi Backend to retrieve the list of users aquariums.
 *
 * @author Stefan Schubert
 */
public class TankServiceImpl implements TankService {
    @Override
    public @NotNull List<AquariumTo> getUsersTanks(@NotNull String JWTAuthtoken) throws BusinessException {

        // FIXME STS (18.04.20): Replace dummy impl.
        AquariumTo aquariumTo1 = new AquariumTo();
        AquariumTo aquariumTo2 = new AquariumTo();

        aquariumTo1.setId(1L);
        aquariumTo2.setId(2L);
        aquariumTo1.setDescription("Test Aqua One");
        aquariumTo2.setDescription("Test Aqua Two");

        ArrayList<AquariumTo> aquariumTos = new ArrayList<>();
        aquariumTos.add(aquariumTo1);
        aquariumTos.add(aquariumTo2);

        return aquariumTos;
    }
}
