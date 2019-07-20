/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.FishRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static de.bluewhale.sabi.util.Mapper.mapFishEntity2To;
import static de.bluewhale.sabi.util.Mapper.mapFishTo2Entity;

/**
 * It's all about Fish here ;-)
 */
@Service
public class FishServiceImpl implements FishService {

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FishRepository fishRepository;

    @Override
    public ResultTo<FishTo> registerNewFish(FishTo pFishTo, UserTo pRegisteredUser) {
        FishTo createdFishTo = null;
        Message message = null;

        Long pFishToId = pFishTo.getId();
        if (pFishToId != null) {
            // ImpotenceCheck: Do not create the same fish twice (identified by id).
            boolean isAlreadyThere = fishRepository.existsById(pFishToId);
            createdFishTo = pFishTo;
            message = Message.error(TankMessageCodes.FISH_ALREADY_EXISTS, createdFishTo.getNickname());
        } else {
            UserEntity userEntity = userRepository.getOne(pRegisteredUser.getId());
            AquariumEntity aquariumEntity = aquariumRepository.getOne(pFishTo.getAquariumId());

            if (userEntity == null) {
                message = Message.error(TankMessageCodes.UNKNOWN_USER, aquariumEntity.getId());
            } else if ((aquariumEntity != null) && (!aquariumEntity.getUser().getId().equals(userEntity.getId()))) {
                message = Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumEntity.getId());
            }

            if (message == null) {
                // Continue only if we got no error so far
                FishEntity fishEntity = new FishEntity();
                mapFishTo2Entity(pFishTo, fishEntity);
                FishEntity createdFishEntity = fishRepository.save(fishEntity);
                createdFishTo = new FishTo();
                mapFishEntity2To(createdFishEntity, createdFishTo);
                message = Message.info(TankMessageCodes.CREATE_SUCCEEDED, fishEntity.getId());
            }
        }

        ResultTo<FishTo> fishToResultTo = new ResultTo<>(createdFishTo, message);
        return fishToResultTo;
    }

    @Override
    public void removeFish(Long pFishId, UserTo pRegisteredUser) {
        // This is to ensure that the user really ones the one he requests for removal.
        FishEntity fishEntity = fishRepository.findUsersFish(pFishId, pRegisteredUser.getId());
        if (fishEntity != null) {
            fishRepository.delete(fishEntity);
        }
    }

    @Override
    public FishTo getUsersFish(Long pFishId, UserTo registeredUser) {
        FishTo fishTo = null;
        FishEntity fishEntity = fishRepository.findUsersFish(pFishId, registeredUser.getId());

        if (fishEntity != null) {
            fishTo =  new FishTo();
            mapFishEntity2To(fishEntity, fishTo);
        }

        return fishTo;
    }
}
