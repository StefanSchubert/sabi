/*
 * Copyright (c) 2016. by Stefan Schubert
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
            aquariumEntity.setSizeUnit(pAquariumTo.getSizeUnit());
            aquariumEntity.setSize(pAquariumTo.getSize());
            aquariumEntity.setDescription(pAquariumTo.getDescription());
            aquariumEntity.setUser(userEntity);
            aquariumEntity.setActive(true);

            AquariumEntity createdAquariumEntity = aquariumDao.create(aquariumEntity);
            createdAquariumTo = pAquariumTo;
            createdAquariumTo.setId(createdAquariumEntity.getId());
            createdAquariumTo.setUserId(createdAquariumEntity.getUser().getId());
            message = Message.info(TankMessageCodes.CREATE_SUCCEEDED, aquariumEntity.getId());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(createdAquariumTo, message);
        return aquariumToResultTo;
    }
}
