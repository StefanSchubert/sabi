/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import de.bluewhale.sabi.util.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides all required for dealing with Plague records here e.g. for use cases around the {@link de.bluewhale.sabi.persistence.model.PlagueRecordEntity}
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class PlagueCenterServiceImpl implements PlagueCenterService {

    @Autowired
    PlagueRecordEntityRepository plagueRecordEntityRepository;

    @Autowired
    PlagueStatusRepository plagueStatusRepository;

    @Autowired
    PlagueRepository plagueRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public List<PlagueRecordTo> listPlaguesRecordsOf(Long pTankID) {
        // TODO STS (28.09.22): impl me
        throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.PlagueRecordTo> listPlaguesRecordsOf([pTankID])");
    }

    @Override
    public List<PlagueTo> listAllPlagueTypes(String pUsersLanguage) {
        List<PlagueTo> plagueTos = new ArrayList<>();
        List<PlagueEntity> knownPlagues = plagueRepository.findAll();
        for (PlagueEntity plagueEntity : knownPlagues) {
            List<LocalizedPlagueEntity> localizedPlagueEntities = plagueEntity.getLocalizedPlagueEntities();
            Optional<LocalizedPlagueEntity> localizedPlagueEntity = localizedPlagueEntities.stream().filter(item -> pUsersLanguage.equalsIgnoreCase(item.getLanguage())).findFirst();
            if (localizedPlagueEntity.isPresent()) {
                PlagueTo plagueTo = new PlagueTo();
                plagueTo.setId(localizedPlagueEntity.get().getPlague_id());
                plagueTo.setCommonName(localizedPlagueEntity.get().getCommonName());
                plagueTo.setScientificName(plagueEntity.getScientificName());
                plagueTos.add(plagueTo);
            }
        }
        return plagueTos;
    }

    @Override
    public List<PlagueStatusTo> listAllPlagueStatus(String pUsersLanguage) {
        List<PlagueStatusTo> plagueStatusTos = new ArrayList<>();
        List<PlagueStatusEntity> knownStatus = plagueStatusRepository.findAll();
        for (PlagueStatusEntity plagueStatusEntity : knownStatus) {
            List<LocalizedPlagueStatusEntity> localizedPlagueStatusEntities = plagueStatusEntity.getLocalizedPlagueStatusEntities();
            Optional<LocalizedPlagueStatusEntity> localizedPlagueStatus = localizedPlagueStatusEntities.stream().filter(item -> pUsersLanguage.equalsIgnoreCase(item.getLanguage())).findFirst();
            if (localizedPlagueStatus.isPresent()) {
                PlagueStatusTo plagueStatusTo = new PlagueStatusTo();
                plagueStatusTo.setId(localizedPlagueStatus.get().getPlague_status_id());
                plagueStatusTo.setDescription(localizedPlagueStatus.get().getDescription());
                plagueStatusTos.add(plagueStatusTo);
            }
        }
        return plagueStatusTos;
    }

    @Override
    public List<PlagueRecordTo> listPlagueRecordsOf(String pUserEmail, Integer resultLimit) {

        List<PlagueRecordEntity> plagueRecordEntityList;
        UserEntity userEntity = userRepository.getByEmail(pUserEmail);

        if (resultLimit == null || resultLimit ==0) {
            plagueRecordEntityList = plagueRecordEntityRepository.findByUserOrderByObservedOnDesc(userEntity);
        } else {
            Pageable page = PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "observedOn"));
            plagueRecordEntityList = plagueRecordEntityRepository.findByUserOrderByObservedOnDesc(userEntity, page);
        }

        List<PlagueRecordTo> plagueRecordToList = mapPlagueRecordEntities2TOs(plagueRecordEntityList);
        return plagueRecordToList;

    }

    @Override
    public ResultTo<PlagueRecordTo> removePlagueRecord(Long pPlagueRecordID, String pUserEmail) {
        // TODO STS (28.09.22): impl me
        throw new UnsupportedOperationException("de.bluewhale.sabi.model.ResultTo<de.bluewhale.sabi.model.PlagueRecordTo> removePlagueRecord([pPlagueRecordID, pUserEmail])");
    }

    @Override
    public ResultTo<PlagueRecordTo> addPlagueRecord(PlagueRecordTo pPlagueRecordTo, String pUserEmail) {

        PlagueRecordTo createdPlagueRecordTo;
        Message message;

        UserEntity requestingUser = userRepository.getByEmail(pUserEmail);
        if (requestingUser == null) {
            message = Message.error(PlagueCenterMessageCodes.UNKNOWN_USER, pUserEmail);
            return new ResultTo<>(pPlagueRecordTo, message);
        }

        // check if tank belongs to provided user
        AquariumEntity aquariumEntity = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(pPlagueRecordTo.getAquariumId(), requestingUser.getId());
        if (aquariumEntity == null) {
            message = Message.error(PlagueCenterMessageCodes.NOT_YOUR_RECORD, pUserEmail);
            return new ResultTo<>(pPlagueRecordTo, message);

        }

        createdPlagueRecordTo = new PlagueRecordTo();
        PlagueRecordEntity plagueRecordEntity = new PlagueRecordEntity();
        Mapper.mapPlagueRecordTo2Entity(pPlagueRecordTo,plagueRecordEntity,aquariumEntity, requestingUser);
        PlagueRecordEntity createdPlagueRecordEntity = plagueRecordEntityRepository.saveAndFlush(plagueRecordEntity);
        Mapper.mapPlagueRecordEntity2To(createdPlagueRecordEntity,createdPlagueRecordTo);
        message = Message.info(PlagueCenterMessageCodes.CREATE_SUCCEEDED, createdPlagueRecordTo.getId());

        ResultTo<PlagueRecordTo> plagueRecordToResultTo = new ResultTo<>(createdPlagueRecordTo, message) ;
        return plagueRecordToResultTo;
    }

    @Override
    public ResultTo<PlagueRecordTo> updatePlagueRecord(PlagueRecordTo pPlagueRecordTo, String pUserEmail) {
        // TODO STS (28.09.22): impl me
        throw new UnsupportedOperationException("de.bluewhale.sabi.model.ResultTo<de.bluewhale.sabi.model.PlagueRecordTo> updatePlagueRecord([pPlagueRecordTo, pUserEmail])");
    }

    @Override
    public String fetchAmountOfPlagueRecords() {
        return String.valueOf(plagueRecordEntityRepository.count());
    }

    private List<PlagueRecordTo> mapPlagueRecordEntities2TOs(List<PlagueRecordEntity> plagueRecordEntityList) {
        List<PlagueRecordTo> plagueRecordToList = new ArrayList<PlagueRecordTo>();
        for (PlagueRecordEntity plagueRecordEntity : plagueRecordEntityList) {
            PlagueRecordTo plagueRecordTo = new PlagueRecordTo();
            Mapper.mapPlagueRecordEntity2To(plagueRecordEntity, plagueRecordTo);
            plagueRecordToList.add(plagueRecordTo);
        }
        return plagueRecordToList;
    }

}
