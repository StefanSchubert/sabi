/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.MeasurementDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.util.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Provides all required services for use cases around the {@link de.bluewhale.sabi.persistence.model.MeasurementEntity}
 *
 * @author Stefan Schubert
 */
@Service
public class MeasurementServiceImpl extends CommonService implements MeasurementService {

    @Autowired
    MeasurementDao measurementDao;

    @Autowired
    UserDao userDao;

    @Autowired
    AquariumDao aquariumDao;

    @Override
    public List<MeasurementTo> listMeasurements(Long pTankID) {
        return measurementDao.listTanksMeasurements(pTankID);
    }

    @Override
    public List<MeasurementTo> listMeasurements(String pUserEmail) {
        UserTo userTo = userDao.loadUserByEmail(pUserEmail);
        if (userTo == null) {
            return Collections.emptyList();
        }
        return measurementDao.findUsersMeasurements(userTo.getId());
    }

    @Override
    public ResultTo<MeasurementTo> removeMeasurement(Long pMeasurementID, String pUserEmail) {
        Message resultMsg;
        MeasurementTo usersMeasurement = null;

        UserTo userTo = userDao.loadUserByEmail(pUserEmail);
        if (userTo == null) {
            resultMsg = Message.error(TankMessageCodes.UNKNOWN_USER, pUserEmail);
            return new ResultTo<>(usersMeasurement,resultMsg);
        }

         usersMeasurement = measurementDao.getUsersMeasurement(pMeasurementID, userTo.getId());

        if (usersMeasurement == null) {
            resultMsg = Message.error(TankMessageCodes.MEASURMENT_ALREADY_DELETED);
        } else {
            resultMsg = Message.info(TankMessageCodes.REMOVAL_SUCCEEDED);
        }

        return new ResultTo<>(usersMeasurement,resultMsg);
    }

    @Override
    public ResultTo<MeasurementTo> addMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {

        Message resultMsg;

        // ensure that tank belongs to user and add measurement
        Long aquariumId = pMeasurementTo.getAquariumId();
        UserTo userTo = userDao.loadUserByEmail(pUserEmail);
        AquariumTo usersAquarium = aquariumDao.getUsersAquarium(aquariumId, userTo.getId());

        if ((usersAquarium != null) && (usersAquarium.getId().equals(aquariumId))) {

            MeasurementEntity measurementEntity = new MeasurementEntity();
            Mapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo,measurementEntity);
            AquariumEntity aquariumEntity = aquariumDao.find(aquariumId);
            measurementEntity.setAquarium(aquariumEntity);

            MeasurementEntity createdMeasurementEntity = measurementDao.create(measurementEntity);
            Mapper.mapMeasurementEntity2To(createdMeasurementEntity, pMeasurementTo);

            resultMsg = Message.info(TankMessageCodes.CREATE_SUCCEEDED);
        } else {
            resultMsg = Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumId);
        }



        return new ResultTo<>(pMeasurementTo,resultMsg);
    }

    @Override
    public ResultTo<MeasurementTo> updateMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {
        Message resultMsg;

        // ensure that measurement belongs to user and update measurement
        UserTo userTo = userDao.loadUserByEmail(pUserEmail);
        MeasurementTo usersMeasurement = measurementDao.getUsersMeasurement(pMeasurementTo.getId(), userTo.getId());

        if (usersMeasurement != null) {

            MeasurementEntity measurementEntity = measurementDao.find(usersMeasurement.getId());
            Mapper.mapMeasurementTo2EntityWithoutAquarium(pMeasurementTo,measurementEntity);
            MeasurementEntity updatedEntity = measurementDao.update(measurementEntity);
            Mapper.mapMeasurementEntity2To(updatedEntity,pMeasurementTo);

            resultMsg = Message.info(TankMessageCodes.UPDATE_SUCCEEDED);

        } else {
            resultMsg = Message.error(TankMessageCodes.NOT_YOUR_MEASUREMENT,pMeasurementTo);
        }

        return new ResultTo<>(pMeasurementTo, resultMsg);

    }

}
