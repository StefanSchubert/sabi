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
    public ResultTo<AquariumTo> updateTank(AquariumTo aquariumTo, UserTo registeredUser) {

        Message message;

        // Check if it's users tank
        AquariumTo tank = getTank(aquariumTo.getId(), registeredUser);

        if (tank != null) {

            AquariumEntity aquariumEntity = aquariumDao.find(tank.getId());
            mapAquariumTo2Entity(aquariumTo,aquariumEntity);
            aquariumDao.update(aquariumEntity);

            message = Message.info(TankMessageCodes.UPDATE_SUCCEEDED, aquariumTo.getDescription());
        } else {
            message = Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumTo.getDescription());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(aquariumTo, message) ;
        return aquariumToResultTo;
    }

    @Override
    public AquariumTo getTank(Long aquariumId, UserTo registeredUser) {

        AquariumEntity aquariumEntity = aquariumDao.find(aquariumId);
        AquariumTo aquariumTo = null;

        if (aquariumEntity != null && aquariumEntity.getUser().getId() == registeredUser.getId()) {

            aquariumTo = new AquariumTo();
            mapAquariumEntity2To(aquariumEntity, aquariumTo);

        } else {
            // TODO STS (16.06.17): Some logging here
        }
        return aquariumTo;
    }

    @Override
    public void removeTank(Long persistedTankId, UserTo registeredUser) {
        AquariumEntity aquariumEntity = aquariumDao.getUsersAquarium(persistedTankId, registeredUser.getId());
        if (aquariumEntity != null) {
            aquariumDao.delete(aquariumEntity.getId());
        }
    }
}
