/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.bluewhale.sabi.util.Mapper.mapAquariumEntity2To;
import static de.bluewhale.sabi.util.Mapper.mapAquariumTo2Entity;

/**
 *
 * User: Stefan Schubert
 * Date: 30.04.16
 */
@Service
public class TankServiceImpl extends CommonService implements TankService {

   @Autowired
   private AquariumDao aquariumDao;

   @Autowired
   private UserDao userDao;


    @Override
    public ResultTo<AquariumTo> registerNewTank(final AquariumTo pAquariumTo, final UserTo pRegisteredUser) {

        AquariumTo createdAquariumTo = null;
        Message message = null;

        Long pAquariumToId = pAquariumTo.getId();
        if (pAquariumToId != null) {
            // Idempotent: Do not create the same tank twice (identified by id).
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






}
