/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.FishDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.bluewhale.sabi.util.Mapper.mapAquariumEntity2To;
import static de.bluewhale.sabi.util.Mapper.mapAquariumTo2Entity;

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
    public ResultTo<AquariumTo> registerNewTank(final AquariumTo pAquariumTo, final String pRegisteredUser) {

        AquariumTo createdAquariumTo = null;
        Message message = null;

        Long pAquariumToId = pAquariumTo.getId();
        if (pAquariumToId != null && (aquariumDao.find(pAquariumToId) != null)) {
            // ImpotenceCheck: Do not create the same tank twice (identified by id).
            createdAquariumTo = pAquariumTo;
            message = Message.error(TankMessageCodes.TANK_ALREADY_EXISTS, pAquariumTo.getDescription());
        } else {
            UserTo userTo = userDao.loadUserByEmail(pRegisteredUser);
            UserEntity userEntity = userDao.find(userTo.getId());
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
    public List<AquariumTo> listTanks(final String pUserEmail) {

        UserTo userTo = userDao.loadUserByEmail(pUserEmail);
        List<AquariumTo> tankList = aquariumDao.findUsersTanks(userTo.getId());

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
    public ResultTo<AquariumTo> updateTank(AquariumTo updatedAquariumTo, String pUser) {

        Message message;

        // Ensure it's users tank
        UserTo requestingUser = userDao.loadUserByEmail(pUser);
        AquariumTo usersAquarium = aquariumDao.getUsersAquarium(updatedAquariumTo.getId(), requestingUser.getId());

        if (usersAquarium != null) {

            AquariumEntity aquariumEntity = aquariumDao.find(usersAquarium.getId());
            mapAquariumTo2Entity(updatedAquariumTo,aquariumEntity);
            AquariumEntity updatedEntity = aquariumDao.update(aquariumEntity);
            mapAquariumEntity2To(updatedEntity,updatedAquariumTo);

            message = Message.info(TankMessageCodes.UPDATE_SUCCEEDED, updatedAquariumTo.getDescription());
        } else {
            message = Message.error(TankMessageCodes.NOT_YOUR_TANK, updatedAquariumTo.getDescription());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(updatedAquariumTo, message) ;
        return aquariumToResultTo;
    }

    @Override
    public AquariumTo getTank(Long aquariumId, String registeredUser) {

        AquariumTo aquariumTo = null;

        UserTo userTo = userDao.loadUserByEmail(registeredUser);
        if (userTo != null) {
            aquariumTo = aquariumDao.getUsersAquarium(aquariumId, userTo.getId());
        }

        return aquariumTo;
    }

    @Override
    public ResultTo<AquariumTo> removeTank(Long persistedTankId, String registeredUser) {
        ResultTo<AquariumTo> resultTo;
        UserTo userTo = userDao.loadUserByEmail(registeredUser);
        if (userTo !=null) {
            AquariumTo aquariumTo = aquariumDao.getUsersAquarium(persistedTankId, userTo.getId());
            if (aquariumTo != null) {
                aquariumDao.delete(aquariumTo.getId());
                resultTo = new ResultTo<>(aquariumTo, Message.info(TankMessageCodes.REMOVAL_SUCCEEDED));
            } else {
                resultTo = new ResultTo<>(aquariumTo, Message.error(TankMessageCodes.NOT_YOUR_TANK));
            }
        } else {
                resultTo = new ResultTo<>(new AquariumTo(), Message.error(TankMessageCodes.UNKNOWN_USER));
        }
        return resultTo;
    }
}
