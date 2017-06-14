/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
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

import java.util.List;

import static de.bluewhale.sabi.util.Mapper.*;

/**
 * User: Stefan Schubert
 * Date: 30.04.16
 */
@Service
public class TankServiceImpl extends CommonService implements TankService {

    @Autowired
    private AquariumDao aquariumDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FishDao fishDao;


    @Override
    public ResultTo<AquariumTo> registerNewTank(final AquariumTo pAquariumTo, final UserTo pRegisteredUser) {

        AquariumTo createdAquariumTo = null;
        Message message = null;

        Long pAquariumToId = pAquariumTo.getId();
        if (pAquariumToId != null) {
            // ImpotenceCheck: Do not create the same tank twice (identified by id).
            AquariumEntity aquariumEntity = aquariumDao.find(pAquariumToId);
            createdAquariumTo = pAquariumTo;
            message = Message.error(TankMessageCodes.TANK_ALREADY_EXISTS, aquariumEntity.getDescription());
        } else {
            UserEntity userEntity = userDao.find(pRegisteredUser.getId());
            AquariumEntity aquariumEntity = new AquariumEntity();
            mapAquariumTo2Entity(pAquariumTo, aquariumEntity);
            aquariumEntity.setUser(userEntity);
            aquariumEntity.setActive(true); // default for new ones

            AquariumEntity createdAquariumEntity = aquariumDao.create(aquariumEntity);

            /*

            // userEntity.getAquariums().add(createdAquariumEntity);

            Does not work. OneToMany misconfigured? The Collection on Users side is empty!
            Trying to set the relation ship from this side fails. JPA seems to expect a n:m for bidirectional navigation then:

            Internal Exception: java.sql.SQLSyntaxErrorException: Table 'sabi.users_aquarium' doesn't exist
            Error Code: 1146
            Call: INSERT INTO users_aquarium (aquariums_ID, UserEntity_ID) VALUES (?, ?)
	        bind => [2 parameters bound]
            Query: DataModifyQuery(name="aquariums" sql="INSERT INTO users_aquarium (aquariums_ID, UserEntity_ID) VALUES (?, ?)")
             */

            createdAquariumTo = new AquariumTo();
            mapAquariumEntity2To(createdAquariumEntity, createdAquariumTo);
            message = Message.info(TankMessageCodes.CREATE_SUCCEEDED, aquariumEntity.getId());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(createdAquariumTo, message);
        return aquariumToResultTo;
    }


    @Override
    public List<AquariumTo> listTanks(final Long pUserId) {

        List<AquariumTo> tankList = aquariumDao.findUsersTanks(pUserId);

        /*
        Bidirectional side does not work. Wrong JPA Setup?

        UserEntity userEntity = userDao.find(pUserId);
        if (userEntity != null) {
            List<AquariumEntity> aquariumEntities = userEntity.getAquariums();
            for (AquariumEntity aquariumEntity : aquariumEntities) {
                AquariumTo aquariumTo = new AquariumTo();
                mapAquariumEntity2To(aquariumEntity,aquariumTo);
                tankList.add(aquariumTo);
            }
        }

         */
        return tankList;
    }

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


}
