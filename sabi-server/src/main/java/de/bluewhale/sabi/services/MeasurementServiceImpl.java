/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UnitEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UnitRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides all required for dealing with measurements here e.g. for use cases around the {@link de.bluewhale.sabi.persistence.model.MeasurementEntity}
 *
 * @author Stefan Schubert
 */
@Service
public class MeasurementServiceImpl implements MeasurementService {

    @Autowired
    MeasurementRepository measurementRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UnitRepository unitRepository;

    @Override
    public List<MeasurementTo> listMeasurements(Long pTankID) {
        AquariumEntity aquarium = aquariumRepository.getOne(pTankID);
        @NotNull List<MeasurementEntity> measurementsOfAquarium = measurementRepository.findByAquarium(aquarium);

        List<MeasurementTo> measurementTos = mapMeasurementEntities2TOs(measurementsOfAquarium);

        return measurementTos;
    }

    private List<MeasurementTo> mapMeasurementEntities2TOs(@NotNull List<MeasurementEntity> measurementsOfAquarium) {
        List<MeasurementTo> measurementTos = new ArrayList<MeasurementTo>();
        for (MeasurementEntity measurementEntity : measurementsOfAquarium) {
            MeasurementTo measurementTo = new MeasurementTo();
            Mapper.mapMeasurementEntity2To(measurementEntity, measurementTo);
            measurementTos.add(measurementTo);
        }
        return measurementTos;
    }

    @Override
    public @NotNull List<UnitTo> listAllMeasurementUnits() {
        List<UnitTo> unitToList = Collections.emptyList();
        List<UnitEntity> unitEntityList = unitRepository.findAll();
        if (unitEntityList != null && !unitEntityList.isEmpty()) {
            unitToList = mapUnitEntities2TOs(unitEntityList);

        }
        return unitToList;
    }

    private List<UnitTo> mapUnitEntities2TOs(@NotNull List<UnitEntity> measurementUnits) {
        List<UnitTo> unitTos = new ArrayList<UnitTo>();
        for (UnitEntity unitEntity : measurementUnits) {
            UnitTo unitTo = new UnitTo();
            Mapper.mapUnitEntity2To(unitEntity, unitTo);
            unitTos.add(unitTo);
        }
        return unitTos;
    }

    @Override
    public List<MeasurementTo> listMeasurements(String pUserEmail, Integer resultLimit) {

        UserEntity user = userRepository.getByEmail(pUserEmail);
        List<MeasurementEntity> measurementsOfUser;

        if (resultLimit == null || resultLimit ==0) {
            measurementsOfUser = measurementRepository.findByUser(user);
        } else {
            Pageable page = PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "measuredOn"));
            measurementsOfUser = measurementRepository.findByUser(user, page);
        }

        List<MeasurementTo> measurementTos = mapMeasurementEntities2TOs(measurementsOfUser);
        return measurementTos;
    }

    @Override
    public ResultTo<MeasurementTo> removeMeasurement(Long pMeasurementID, String pUserEmail) {
        Message resultMsg;
        MeasurementTo usersMeasurementTo = new MeasurementTo();


        UserEntity user = userRepository.getByEmail(pUserEmail);
        if (user == null) {
            resultMsg = Message.error(TankMessageCodes.UNKNOWN_USER, pUserEmail);
            return new ResultTo<>(usersMeasurementTo, resultMsg);
        }

        MeasurementEntity usersMeasurementEntity = measurementRepository.getByIdAndUser(pMeasurementID, user);
        // usersMeasurement = measurementDao.getUsersMeasurement(pMeasurementID, userTo.getId());

        if (usersMeasurementEntity == null) {
            resultMsg = Message.error(TankMessageCodes.MEASURMENT_ALREADY_DELETED);
        } else {
            measurementRepository.delete(usersMeasurementEntity);
            Mapper.mapMeasurementEntity2To(usersMeasurementEntity, usersMeasurementTo);
            resultMsg = Message.info(TankMessageCodes.REMOVAL_SUCCEEDED);
        }

        return new ResultTo<>(usersMeasurementTo, resultMsg);
    }

    @Override
    public ResultTo<MeasurementTo> addMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {

        Message resultMsg;

        // ensure that tank belongs to user and add measurement
        Long aquariumId = pMeasurementTo.getAquariumId();

        UserEntity user = userRepository.getByEmail(pUserEmail);
        if (user == null) {
            resultMsg = Message.error(TankMessageCodes.UNKNOWN_USER, pUserEmail);
            return new ResultTo<>(pMeasurementTo, resultMsg);
        }

        AquariumEntity usersAquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());

        if (usersAquarium != null) {

            MeasurementEntity measurementEntity = new MeasurementEntity();
            Mapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo, measurementEntity);
            measurementEntity.setAquarium(usersAquarium);
            measurementEntity.setUser(user);

            MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);
            Mapper.mapMeasurementEntity2To(createdMeasurementEntity, pMeasurementTo);

            resultMsg = Message.info(TankMessageCodes.CREATE_SUCCEEDED);
        } else {
            resultMsg = Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumId);
        }

        return new ResultTo<>(pMeasurementTo, resultMsg);
    }

    @Override
    public ResultTo<MeasurementTo> updateMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {
        Message resultMsg;

        // ensure that measurement belongs to user and update measurement
        UserEntity user = userRepository.getByEmail(pUserEmail);
        if (user == null) {
            resultMsg = Message.error(TankMessageCodes.UNKNOWN_USER, pUserEmail);
            return new ResultTo<>(pMeasurementTo, resultMsg);
        }

        MeasurementEntity measurementEntity = measurementRepository.getByIdAndUser(pMeasurementTo.getId(), user);

        if (measurementEntity != null) {

            Mapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo, measurementEntity);
            MeasurementEntity updatedEntity = measurementRepository.save(measurementEntity);
            Mapper.mapMeasurementEntity2To(updatedEntity, pMeasurementTo);

            resultMsg = Message.info(TankMessageCodes.UPDATE_SUCCEEDED);

        } else {
            resultMsg = Message.error(TankMessageCodes.NOT_YOUR_MEASUREMENT, pMeasurementTo);
        }

        return new ResultTo<>(pMeasurementTo, resultMsg);
    }

}
