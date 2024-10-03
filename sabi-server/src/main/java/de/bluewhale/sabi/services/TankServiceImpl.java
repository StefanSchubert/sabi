/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.AquariumMapper;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Stefan Schubert
 * Date: 30.04.16
 */
@Service
public class TankServiceImpl implements TankService {

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private AquariumMapper aquariumMapper;

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
        if (pAquariumToId != null && aquariumRepository.existsById(pAquariumToId)) {
            // ImpotenceCheck: Do not create the same tank twice (identified by id).
            createdAquariumTo = pAquariumTo;
            message = Message.error(TankMessageCodes.TANK_ALREADY_EXISTS, pAquariumTo.getDescription());
        } else {

            AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(pAquariumTo);
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

            createdAquariumTo = aquariumMapper.mapAquariumEntity2To(createdAquariumEntity);
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
            AquariumTo aquariumTo = aquariumMapper.mapAquariumEntity2To(aquariumEntity);
            aquariumTos.add(aquariumTo);
        }

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
            // FIXME STS (04.09.23): The Mapping here will provide a completely new entity
            // however we have the aquarium before. Isn't there a merge mapping
            // between entities available by mapstruts?
            aquariumEntity = aquariumMapper.mapAquariumTo2Entity(updatedAquariumTo);
            aquariumEntity.setUser(requestingUser);
            AquariumEntity updatedEntity = aquariumRepository.saveAndFlush(aquariumEntity);
            updatedAquariumTo = aquariumMapper.mapAquariumEntity2To(updatedEntity);
            message = Message.info(TankMessageCodes.UPDATE_SUCCEEDED, updatedAquariumTo.getDescription());
        } else {
            message = Message.error(TankMessageCodes.NOT_YOUR_TANK, updatedAquariumTo.getDescription());
        }

        ResultTo<AquariumTo> aquariumToResultTo = new ResultTo<>(updatedAquariumTo, message);
        return aquariumToResultTo;
    }

    @Override
    public AquariumTo getTank(Long aquariumId, String pUsersEmail) {

        AquariumTo aquariumTo = null;

        UserEntity user = userRepository.getByEmail(pUsersEmail);

        if (user != null) {
            AquariumEntity usersAquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
            if (usersAquarium != null) {
                aquariumTo = aquariumMapper.mapAquariumEntity2To(usersAquarium);
            }
        }

        return aquariumTo;
    }

    @Override
    public ResultTo<AquariumTo> removeTank(Long persistedTankId, String pUsersEmail) {
        ResultTo<AquariumTo> resultTo;
        UserEntity user = userRepository.getByEmail(pUsersEmail);
        if (user != null) {
            AquariumEntity usersAquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(persistedTankId, user.getId());
            AquariumTo aquariumTo = new AquariumTo();
            aquariumTo.setId(persistedTankId);

            if (usersAquarium != null) {
                aquariumRepository.delete(usersAquarium);
                aquariumTo = aquariumMapper.mapAquariumEntity2To(usersAquarium);
                resultTo = new ResultTo<>(aquariumTo, Message.info(TankMessageCodes.REMOVAL_SUCCEEDED));
            } else {
                resultTo = new ResultTo<>(aquariumTo, Message.error(TankMessageCodes.NOT_YOUR_TANK));
            }
        } else {
            resultTo = new ResultTo<>(new AquariumTo(), Message.error(TankMessageCodes.UNKNOWN_USER));
        }
        return resultTo;
    }

    @Override
    public String fetchAmountOfTanks() {
        return String.valueOf(aquariumRepository.count());
    }

    @Override
    public String fetchAmountOfTanksWithAPIKeyUsage() {
        return String.valueOf(aquariumRepository.countAquariumEntitiesByTemperatureApiKeyNotNull());
    }

    @Override
    public AquariumTo getTankForTemperatureApiKey(String apiKey) {
        AquariumTo aquariumTo = null;
        AquariumEntity aquariumEntity = aquariumRepository.getAquariumEntityByTemperatureApiKeyEquals(apiKey);
        if (aquariumEntity != null) {
            aquariumTo = aquariumMapper.mapAquariumEntity2To(aquariumEntity);
        }
        return aquariumTo;
    }

    @Override
    public ResultTo<AquariumTo> generateAndAssignNewTemperatureApiKey(Long persistedTankId, String pUsersEmail) {

        UserEntity user = userRepository.getByEmail(pUsersEmail);
        AquariumEntity usersTankEntity = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(persistedTankId, user.getId());
        if (usersTankEntity != null) {

            String apiKey = generateNewApiKey();
            // Ensure key is unique - and not already assigned. It will generate a new one until it's unique
            while (aquariumRepository.getAquariumEntityByTemperatureApiKeyEquals(apiKey) != null) {
                apiKey = generateNewApiKey();
            }

            usersTankEntity.setTemperatureApiKey(apiKey);

            AquariumTo aquariumTo = aquariumMapper.mapAquariumEntity2To(usersTankEntity);
            return new ResultTo<>(aquariumTo, Message.info(TankMessageCodes.UPDATE_SUCCEEDED));

        } else {
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK));
        }
    }


    /**
     * generate Key - using Passay as it's already in the project
     * @return 30 digit Key
     */
    private static String generateNewApiKey() {
        CharacterRule digits = new CharacterRule(EnglishCharacterData.Digit);
        CharacterRule alphabets = new CharacterRule(EnglishCharacterData.Alphabetical);
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        return passwordGenerator.generatePassword(30, digits, alphabets);
    }
}
