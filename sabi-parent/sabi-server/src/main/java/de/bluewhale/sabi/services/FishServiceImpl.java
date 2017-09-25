/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.FishDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static de.bluewhale.sabi.util.Mapper.mapFishEntity2To;
import static de.bluewhale.sabi.util.Mapper.mapFishTo2Entity;

/**
 * It's all about Fish here ;-)
 */
@Service
public class FishServiceImpl extends CommonService implements FishService {

    @Autowired
    private AquariumDao aquariumDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FishDao fishDao;

    @Override
    public ResultTo<FishTo> registerNewFish(FishTo pFishTo, UserTo pRegisteredUser) {
        FishTo createdFishTo = null;
        Message message = null;

        Long pFishToId = pFishTo.getId();
        if (pFishToId != null) {
            // ImpotenceCheck: Do not create the same fish twice (identified by id).
            FishEntity fishEntity = fishDao.find(pFishToId);
            createdFishTo = pFishTo;
            message = Message.error(TankMessageCodes.FISH_ALREADY_EXISTS, fishEntity.getNickname());
        } else {
            UserEntity userEntity = userDao.find(pRegisteredUser.getId());
            AquariumEntity aquariumEntity = aquariumDao.find(pFishTo.getAquariumId());

            if (userEntity == null) {
                message = Message.error(TankMessageCodes.UNKNOWN_USER, aquariumEntity.getId());
            } else if ((aquariumEntity != null) && (aquariumEntity.getUser().getId() != userEntity.getId())) {
                message = Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumEntity.getId());
            }

            if (message == null) {
                // Continue only if we got no error so far
                FishEntity fishEntity = new FishEntity();
                mapFishTo2Entity(pFishTo, fishEntity);
                FishEntity createdFishEntity = fishDao.create(fishEntity);
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
        FishEntity fishEntity = fishDao.findUsersFish(pFishId, pRegisteredUser.getId());
        if (fishEntity != null) {
            fishDao.delete(fishEntity.getId());
        }
    }

    @Override
    public FishTo getUsersFish(Long pFishId, UserTo registeredUser) {
        FishTo fishTo = new FishTo();
        FishEntity fishEntity = fishDao.findUsersFish(pFishId, registeredUser.getId());

        if (fishEntity != null) {
            mapFishEntity2To(fishEntity, fishTo);
        }

        return fishTo;
    }
}
