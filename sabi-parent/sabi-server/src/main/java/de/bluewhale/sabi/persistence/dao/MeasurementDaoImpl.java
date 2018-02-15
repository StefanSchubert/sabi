/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.util.Mapper;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Specialized DAO Methods of Measurements, which are not provided through the standard CRUD impl.
 *
 * @author Stefan Schubert
 */
@Repository("measurementDao")
public class MeasurementDaoImpl extends GenericDaoImpl<MeasurementEntity> implements MeasurementDao {


    @Override
    public List<MeasurementTo> findUsersMeasurements(Long pUserId) {
        List<MeasurementTo> measurementTos = new ArrayList<MeasurementTo>();

        if (pUserId != null) {
            Query query = em.createNamedQuery("Measurement.getUsersMeasurements");
            query.setParameter("pUserID", pUserId);
            List<MeasurementEntity> MeasurementEntities = query.getResultList();

            for (MeasurementEntity MeasurementEntity : MeasurementEntities) {
                MeasurementTo MeasurementTo = new MeasurementTo();
                Mapper.mapMeasurementEntity2To(MeasurementEntity, MeasurementTo);
                measurementTos.add(MeasurementTo);
            }
        }
        return measurementTos;
    }

    @Override
    public MeasurementTo getUsersMeasurement(Long pPersistedMeasurementId, Long pUserId) {

        MeasurementEntity measurementEntity = find(pPersistedMeasurementId);
        if ((measurementEntity != null) && isUserOwnerOfMeasurement(pUserId, measurementEntity)) {
            MeasurementTo measurementTo = new MeasurementTo();
            Mapper.mapMeasurementEntity2To(measurementEntity, measurementTo);
            return measurementTo;
        } else {
            return null;
        }
    }

    @Override
    public List<MeasurementTo> listTanksMeasurements(Long pTankID) {
        List<MeasurementTo> tanksMeasurements;
        if (pTankID != null) {
            Query query = em.createNamedQuery("Measurement.getAllMeasurementsForTank");
            query.setParameter("pTankID", pTankID);
            List<MeasurementEntity> measurementEntities = query.getResultList();

            tanksMeasurements = new ArrayList<MeasurementTo>(measurementEntities.size());
            for (MeasurementEntity measurementEntity : measurementEntities) {
                MeasurementTo measurementTo = new MeasurementTo();
                Mapper.mapMeasurementEntity2To(measurementEntity,measurementTo);
                tanksMeasurements.add(measurementTo);
            }
            return tanksMeasurements;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isUserOwnerOfMeasurement(Long pUserId, MeasurementEntity measurementEntity) {
        return measurementEntity.getAquarium().getUser().getId().equals(pUserId);
    }
}
