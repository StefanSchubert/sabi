/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.bluewhale.sabi.util.Mapper.mapAquariumEntity2To;
import static de.bluewhale.sabi.util.Mapper.mapAquariumTo2Entity;

/**
 * User: Stefan Schubert
 * Date: 30.04.16
 */
@Service
public class TankServiceImpl implements TankService {

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResultTo<AquariumTo> registerNewTank(final AquariumTo pAquariumTo, final String pRegisteredUsersEmail) {

        AquariumTo createdAquariumTo = null;
        Message message = null;

        UserEntity user = userRepository.getByEmail(pRegisteredUsersEmail);
        if (user == null) {
            message = Message.error(TankMessageCodes.UNKNOWN_USER, pRegisteredUsersEmail);
            return new ResultTo<>(pAquariumTo, message);
        }

        Long pAquariumToId = pAquariumTo.getId();
        if (pAquariumToId != null && (aquariumRepository.existsById(pAquariumToId) == true)) {
            // ImpotenceCheck: Do not create the same tank twice (identified by id).
            createdAquariumTo = pAquariumTo;
            message = Message.error(TankMessageCodes.TANK_ALREADY_EXISTS, pAquariumTo.getDescription());
        } else {

            AquariumEntity aquariumEntity = new AquariumEntity();
            mapAquariumTo2Entity(pAquariumTo, aquariumEntity);
            aquariumEntity.setUser(user);
            aquariumEntity.setActive(true); // default for new ones

            AquariumEntity createdAquariumEntity = aquariumRepository.saveAndFlush(aquariumEntity);

            /*
            // todo update: old comment before refactoring to repositories, needs to be rechecked
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

        UserEntity user = userRepository.getByEmail(pUserEmail);
        if (user == null) {
           return Collections.emptyList();
        }

        @NotNull List<AquariumEntity> usersAquariums = aquariumRepository.findAllByUser_IdIs(user.getId());

        List<AquariumTo> aquariumTos = new ArrayList<>();
        for (AquariumEntity aquariumEntity : usersAquariums) {
            AquariumTo aquariumTo = new AquariumTo();
            Mapper.mapAquariumEntity2To(aquariumEntity,aquariumTo);
            aquariumTos.add(aquariumTo);
        }

        /*
                    // todo update: old comment before refactoring to repositories, needs to be rechecked
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
        return aquariumTos;
    }

    @Override
    public ResultTo<AquariumTo> updateTank(AquariumTo updatedAquariumTo, String pUsersEmail) {

        Message message;

        // Ensure it's users tank
        UserEntity requestingUser = userRepository.getByEmail(pUsersEmail);
        if (requestingUser == null) {
            message = Message.error(TankMessageCodes.UNKNOWN_USER, pUsersEmail);
            return new ResultTo<>(updatedAquariumTo, message);
        }
        AquariumEntity aquariumEntity = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(updatedAquariumTo.getId(), requestingUser.getId());

        if (aquariumEntity != null) {
            mapAquariumTo2Entity(updatedAquariumTo,aquariumEntity);
            AquariumEntity updatedEntity = aquariumRepository.saveAndFlush(aquariumEntity);
            mapAquariumEntity2To(updatedEntity,updatedAquariumTo);
            message = Message.info(TankMessageCodes.UPDATE_SUCCEEDED, updatedAquariumTo.getDescription());
        } else {
            message = Message.error(TankMessageCodes.NOT_YOUR_TANK, updatedAquariumTo.getDescription());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(updatedAquariumTo, message) ;
        return aquariumToResultTo;
    }

    @Override
    public AquariumTo getTank(Long aquariumId, String pUsersEmail) {

        AquariumTo aquariumTo = null;

        UserEntity user = userRepository.getByEmail(pUsersEmail);

        if (user != null) {
            AquariumEntity usersAquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
            if (usersAquarium != null) {
                aquariumTo = new AquariumTo();
                Mapper.mapAquariumEntity2To(usersAquarium,aquariumTo);
            }

        }

        return aquariumTo;
    }

    @Override
    public ResultTo<AquariumTo> removeTank(Long persistedTankId, String pUsersEmail) {
        ResultTo<AquariumTo> resultTo;
        UserEntity user = userRepository.getByEmail(pUsersEmail);
        if (user !=null) {
            AquariumEntity usersAquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(persistedTankId, user.getId());
            AquariumTo aquariumTo = new AquariumTo();
            aquariumTo.setId(persistedTankId);

            if (usersAquarium != null) {
                aquariumRepository.delete(usersAquarium);
                Mapper.mapAquariumEntity2To(usersAquarium,aquariumTo);
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
