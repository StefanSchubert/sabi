/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.util.Mapper;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Specialized DAO Methods of Aquarium, which are not provided through the standard CRUD impl.
 *
 * @author Stefan Schubert
 */
@Repository("aquariumDao")
public class AquariumDaoImpl extends GenericDaoImpl<AquariumEntity> implements AquariumDao {


    @Override
    public List<AquariumTo> findUsersTanks(@NotNull final Long pUserId) {
        ArrayList<AquariumTo> aquariumTos = new ArrayList<AquariumTo>();

        if (pUserId != null) {
            Query query = em.createQuery("select a FROM AquariumEntity a where a.user.id = :userID");
            query.setParameter("userID", pUserId);
            List<AquariumEntity> aquariumEntities = query.getResultList();

            for (AquariumEntity aquariumEntity : aquariumEntities) {
                AquariumTo aquariumTo = new AquariumTo();
                Mapper.mapAquariumEntity2To(aquariumEntity, aquariumTo);
                aquariumTos.add(aquariumTo);
            }

        }

        return aquariumTos;
    }
}
