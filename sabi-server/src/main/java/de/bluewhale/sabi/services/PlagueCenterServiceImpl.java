/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.PlagueRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.PlagueRecordEntityRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    UserRepository userRepository;

    @Override
    public List<PlagueRecordTo> listPlaguesRecordsOf(Long pTankID) {

        // TODO STS (28.09.22): impl me
        return Collections.emptyList();
    }

    @Override
    public List<PlagueTo> listAllPlagueTypes(String usersLanguage) {
        // TODO STS (28.09.22): impl me
        return Collections.emptyList();
    }

    @Override
    public List<PlagueStatusTo> listAllPlagueStatus(String usersLanguage) {
        // TODO STS (28.09.22): impl me
        return Collections.emptyList();
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
        return null;
    }

    @Override
    public ResultTo<PlagueRecordTo> addPlagueRecord(PlagueRecordTo pPlagueRecordTo, String pUserEmail) {
        // TODO STS (28.09.22): impl me
        return null;
    }

    @Override
    public ResultTo<PlagueRecordTo> updatePlagueRecord(PlagueRecordTo pPlagueRecordTo, String pUserEmail) {
        // TODO STS (28.09.22): impl me
        return null;
    }

    @Override
    public String fetchAmountOfPlagueRecords() {
        // TODO STS (28.09.22): impl me
        return "0";
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
