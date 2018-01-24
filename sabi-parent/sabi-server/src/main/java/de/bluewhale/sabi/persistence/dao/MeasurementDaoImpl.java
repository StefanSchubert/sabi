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
        Query query = em.createNamedQuery("Measurement.getMeasurement");
        query.setParameter("pUserID", pUserId);
        query.setParameter("pTankID", pPersistedMeasurementId);

        MeasurementEntity MeasurementEntity = null;
        try {
            MeasurementEntity = (MeasurementEntity) query.getSingleResult();
        } catch (Exception e) {
            // todo some logging here (Idempotence double remove or fraud detection)
        }
        if (MeasurementEntity != null) {
            MeasurementTo MeasurementTo = new MeasurementTo();
            Mapper.mapMeasurementEntity2To(MeasurementEntity, MeasurementTo);
            return MeasurementTo;
        } else {
            return null;
        }
    }
}