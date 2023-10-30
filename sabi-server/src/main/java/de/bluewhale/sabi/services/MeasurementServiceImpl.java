/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */


package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.MeasurementMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import de.bluewhale.sabi.util.Mapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.Mapper.*;

/**
 * Provides all required for dealing with measurements here e.g. for use cases around the {@link de.bluewhale.sabi.persistence.model.MeasurementEntity}
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class MeasurementServiceImpl implements MeasurementService {

    @Autowired
    MeasurementRepository measurementRepository;

    @Autowired
    MeasurementMapper measurementMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UnitRepository unitRepository;

    @Autowired
    ParameterRepository parameterRepository;

    @Autowired
    UserMeasurementReminderRepository measurementReminderRepository;

    @Override
    public List<MeasurementTo> listMeasurements(Long pTankID) {
        AquariumEntity aquarium = aquariumRepository.getOne(pTankID);
        @NotNull List<MeasurementEntity> measurementsOfAquarium = measurementRepository.findByAquarium(aquarium);

        List<MeasurementTo> measurementTos = measurementMapper.mapMeasurementEntities2TOs(measurementsOfAquarium);

        return measurementTos;
    }

    @Override
    public List<MeasurementTo> listMeasurementsFilteredBy(Long pTankID, Integer pUnitID) {
        AquariumEntity aquarium = aquariumRepository.getOne(pTankID);
        @NotNull List<MeasurementEntity> measurementsOfAquarium = measurementRepository.findByAquariumAndUnitIdOrderByMeasuredOnAsc(aquarium, pUnitID);

        List<MeasurementTo> measurementTos = measurementMapper.mapMeasurementEntities2TOs(measurementsOfAquarium);

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


    @Override
    public List<MeasurementTo> listMeasurements(String pUserEmail, Integer resultLimit) {

        UserEntity user = userRepository.getByEmail(pUserEmail);
        List<MeasurementEntity> measurementsOfUser;

        if (resultLimit == null || resultLimit == 0) {
            measurementsOfUser = measurementRepository.findByUserOrderByMeasuredOnDesc(user);
        } else {
            Pageable page = PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "measuredOn"));
            measurementsOfUser = measurementRepository.findByUserOrderByMeasuredOnDesc(user, page);
        }

        List<MeasurementTo> measurementTos = measurementMapper.mapMeasurementEntities2TOs(measurementsOfUser);
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
            usersMeasurementTo = measurementMapper.mapMeasurementEntity2To(usersMeasurementEntity);
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

            MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo);
            measurementEntity.setAquarium(usersAquarium);
            measurementEntity.setUser(user);

            MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);
            pMeasurementTo = measurementMapper.mapMeasurementEntity2To(createdMeasurementEntity);

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

            measurementMapper.mergeMeasurementTo2EntityWithoutAquarium(pMeasurementTo, measurementEntity);
            MeasurementEntity updatedEntity = measurementRepository.save(measurementEntity);
            pMeasurementTo = measurementMapper.mapMeasurementEntity2To(updatedEntity);

            resultMsg = Message.info(TankMessageCodes.UPDATE_SUCCEEDED);

        } else {
            resultMsg = Message.error(TankMessageCodes.NOT_YOUR_MEASUREMENT, pMeasurementTo);
        }

        return new ResultTo<>(pMeasurementTo, resultMsg);
    }

    @Override
    public ParameterTo fetchParameterInfoFor(Integer pUnitID) {

        if (pUnitID == null) {
            log.warn("Tried to fetch Parameter Info for null unit. This smells after a logic flaw.");
            return null;
        }

        ParameterEntity parameterEntity = parameterRepository.findByBelongingUnitIdEquals(pUnitID);

        if (parameterEntity == null) {
            log.warn("Requested parameter info for unitID «{}» is not availabe. Data maintenance recommended.", pUnitID);
            return null;
        }

        ParameterTo parameterTo = new ParameterTo();
        Mapper.mapParameterEntity2To(parameterEntity, parameterTo);

        return parameterTo;
    }

    @Override
    public String fetchAmountOfMeasurements() {
        UserEntity testuser = userRepository.getByEmail(AppConfig.TESTUSER_MAIL);
        if (testuser != null) {
            return String.valueOf(measurementRepository.countAllByUserIsNot(testuser));
        } else {
            return String.valueOf(measurementRepository.count());
        }
    }

    @Override
    public LocalDateTime getLastTimeOfMeasurementTakenFilteredBy(Long pTankID, Integer pUnitID) {

        MeasurementEntity measurement = measurementRepository.findTopByAquarium_IdAndUnitIdOrderByMeasuredOnDesc(pTankID, pUnitID);
        if (measurement != null) {
            return measurement.getMeasuredOn();
        } else {
            return null;
        }
    }

    @Override
    public ResultTo<MeasurementTo> addIotAuthorizedMeasurement(MeasurementTo pMeasurementTo) {

        Message resultMsg;

        Optional<AquariumEntity> optionalAquarium = aquariumRepository.findById(pMeasurementTo.getAquariumId());
        if (optionalAquarium.isPresent()) {

            MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo);
            measurementEntity.setAquarium(optionalAquarium.get());
            measurementEntity.setUser(optionalAquarium.get().getUser());

            MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);
            pMeasurementTo = measurementMapper.mapMeasurementEntity2To(createdMeasurementEntity);

            resultMsg = Message.info(TankMessageCodes.CREATE_SUCCEEDED);

        } else {
            resultMsg = Message.error(TankMessageCodes.UNKNOWN_OR_INACTIVE_TANK);

        }

        return new ResultTo<>(pMeasurementTo, resultMsg);
    }

    @Override
    public List<MeasurementReminderTo> fetchUsersNextMeasurements(String pUserEmail) {
        List<MeasurementReminderTo> measurementReminderTos = new ArrayList<>();

        List<UnitTo> allMeasurementUnits = listAllMeasurementUnits();
        UserEntity user = userRepository.getByEmail(pUserEmail);
        List<UserMeasurementReminderEntity> userMeasurementReminders = user.getUserMeasurementReminders();

        if (userMeasurementReminders != null) {

            for (UserMeasurementReminderEntity reminderEntity : userMeasurementReminders) {

                MeasurementReminderTo reminderTo = new MeasurementReminderTo();

                reminderTo.setUserId(user.getId());
                reminderTo.setUnitId(reminderEntity.getUnitId());
                reminderTo.setPastDays(reminderEntity.getPastdays());
                reminderTo.setActive(reminderEntity.isActive());

                Optional<UnitTo> optionalUnitTo = allMeasurementUnits.stream().filter(item -> item.getId().equals(reminderEntity.getUnitId())).findFirst();
                if (optionalUnitTo.isPresent()) {
                    reminderTo.setUnitName(optionalUnitTo.get().getUnitSign());
                } else {
                    reminderTo.setUnitName("N/A?");
                }

                Optional<MeasurementEntity> lastRecentMeasurement = measurementRepository.findTopByUserAndUnitIdOrderByMeasuredOnDesc(user, reminderEntity.getUnitId());
                if (lastRecentMeasurement.isPresent()) {
                    LocalDateTime nextMeasureDate = lastRecentMeasurement.get().getMeasuredOn().plusDays(reminderEntity.getPastdays());
                    reminderTo.setNextMeasureDate(nextMeasureDate);
                } else {
                    reminderTo.setNextMeasureDate(LocalDateTime.now());
                }

                measurementReminderTos.add(reminderTo);
            }
        }
        return measurementReminderTos;
    }

    @Override
    public ResultTo<MeasurementReminderTo> addMeasurementReminder(MeasurementReminderTo pReminderTo, String pUserEmail) {

        Message resultMsg;
        UserEntity userEntity = userRepository.getByEmail(pUserEmail);
        pReminderTo.setUserId(userEntity.getId());
        pReminderTo.setActive(true);

        // check if it already exists
        Optional<UserMeasurementReminderEntity> optionalReminderEntity = measurementReminderRepository.findTopByUserAndUnitId(userEntity, pReminderTo.getUnitId());

        if (optionalReminderEntity.isPresent()) {
            resultMsg = Message.warning(UserSpecificMessageCodes.RECORD_ALREADY_EXISTS);
            mapUserMeasurementReminderEntity2TO(optionalReminderEntity.get(), pReminderTo);
        } else {
            // if not add it
            UserMeasurementReminderEntity reminderEntity = new UserMeasurementReminderEntity();
            mapUserMeasurementReminderTO2Entity(pReminderTo, reminderEntity, userEntity);

            UserMeasurementReminderEntity savedReminderEntity = measurementReminderRepository.save(reminderEntity);
            userEntity.getUserMeasurementReminders().add(savedReminderEntity); // update cached reminderlist on users side.
            mapUserMeasurementReminderEntity2TO(savedReminderEntity, pReminderTo);
            resultMsg = Message.info(UserSpecificMessageCodes.CREATE_SUCCEEDED);
        }

        return new ResultTo<>(pReminderTo, resultMsg);
    }

    @Override
    public ResultTo<MeasurementReminderTo> updateMeasurementReminder(MeasurementReminderTo pReminderTo, String pUserEmail) {

        Message resultMsg;
        UserEntity userEntity = userRepository.getByEmail(pUserEmail);
        if (pReminderTo != null && pReminderTo.getUserId() != userEntity.getId()) {
            resultMsg = Message.warning(UserSpecificMessageCodes.NOT_YOUR_RECORD);
        } else {
            // check if it already exists
            Optional<UserMeasurementReminderEntity> optionalReminderEntity = measurementReminderRepository.findTopByUserAndUnitId(userEntity, pReminderTo.getUnitId());
            if (optionalReminderEntity.isPresent()) {
                mapUserMeasurementReminderTO2Entity(pReminderTo, optionalReminderEntity.get(), userEntity);
                // no saving required, as entity is attached.
                resultMsg = Message.info(UserSpecificMessageCodes.UPDATE_SUCCEEDED);
            } else {
                // if not add it
                resultMsg = Message.info(UserSpecificMessageCodes.UNKOWN_RECORD);
            }
        }

        return new ResultTo<>(pReminderTo, resultMsg);
    }

    @Override
    public ResultTo<MeasurementReminderTo> deleteMeasurementReminder(MeasurementReminderTo pReminderTo, String pUserEmail) {
        Message resultMsg;
        UserEntity userEntity = userRepository.getByEmail(pUserEmail);
        if (pReminderTo != null && pReminderTo.getUserId() != userEntity.getId()) {
            resultMsg = Message.warning(UserSpecificMessageCodes.NOT_YOUR_RECORD);
        } else {
            // look it up
            Optional<UserMeasurementReminderEntity> optionalReminderEntity = measurementReminderRepository.findTopByUserAndUnitId(userEntity, pReminderTo.getUnitId());
            if (optionalReminderEntity.isPresent()) {
                measurementReminderRepository.delete(optionalReminderEntity.get());
                resultMsg = Message.warning(UserSpecificMessageCodes.REMOVAL_SUCCEEDED);
            } else {
                resultMsg = Message.warning(UserSpecificMessageCodes.UNKOWN_RECORD);
            }
        }
        return new ResultTo<>(pReminderTo, resultMsg);
    }
}
