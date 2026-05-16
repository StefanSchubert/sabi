/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.AquariumEventMapper;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.AquariumEventEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumEventRepository;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link AquariumEventService}.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AquariumEventServiceImpl implements AquariumEventService {

    @Autowired
    AquariumEventRepository aquariumEventRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AquariumEventMapper aquariumEventMapper;

    // -----------------------------------------------------------------------

    /**
     * Returns the resolved AquariumEntity if it belongs to the user, null otherwise.
     */
    private AquariumEntity resolveOwnedAquarium(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        return aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
    }

    @Override
    public List<AquariumEventTo> listEventsForTank(Long aquariumId, String userEmail) {
        AquariumEntity aquarium = resolveOwnedAquarium(aquariumId, userEmail);
        if (aquarium == null) {
            log.warn("listEventsForTank: ownership check failed for aquarium_id={}", aquariumId);
            return Collections.emptyList();
        }
        List<AquariumEventEntity> entities = aquariumEventRepository.findByAquariumIdOrderByEventDateDesc(aquariumId);
        return aquariumEventMapper.mapEntitiesToTos(entities);
    }

    @Override
    @Transactional
    public ResultTo<AquariumEventTo> createEvent(Long aquariumId, AquariumEventTo eventTo, String userEmail) {
        AquariumEntity aquarium = resolveOwnedAquarium(aquariumId, userEmail);
        if (aquarium == null) {
            log.warn("createEvent: ownership check failed for aquarium_id={}", aquariumId);
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumId));
        }
        AquariumEventEntity entity = aquariumEventMapper.mapToToEntity(eventTo);
        entity.setAquariumId(aquariumId);
        entity.setId(null); // ensure client-supplied ID is not used
        AquariumEventEntity saved = aquariumEventRepository.save(entity);
        log.debug("createEvent: saved event_id={} for aquarium_id={}", saved.getId(), aquariumId);
        return new ResultTo<>(aquariumEventMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.CREATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<AquariumEventTo> updateEvent(Long aquariumId, Long eventId, AquariumEventTo eventTo, String userEmail) {
        AquariumEntity aquarium = resolveOwnedAquarium(aquariumId, userEmail);
        if (aquarium == null) {
            log.warn("updateEvent: ownership check failed for aquarium_id={}", aquariumId);
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumId));
        }
        Optional<AquariumEventEntity> existing = aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId);
        if (existing.isEmpty()) {
            log.warn("updateEvent: event_id={} not found in aquarium_id={}", eventId, aquariumId);
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, eventId));
        }
        AquariumEventEntity entity = existing.get();
        aquariumEventMapper.mergeToIntoEntity(eventTo, entity);
        // Let ObjectOptimisticLockingFailureException propagate to controller for HTTP 409
        AquariumEventEntity saved = aquariumEventRepository.save(entity);
        log.debug("updateEvent: updated event_id={} for aquarium_id={}", eventId, aquariumId);
        return new ResultTo<>(aquariumEventMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<AquariumEventTo> deleteEvent(Long aquariumId, Long eventId, String userEmail) {
        AquariumEntity aquarium = resolveOwnedAquarium(aquariumId, userEmail);
        if (aquarium == null) {
            log.warn("deleteEvent: ownership check failed for aquarium_id={}", aquariumId);
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, aquariumId));
        }
        Optional<AquariumEventEntity> existing = aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId);
        if (existing.isEmpty()) {
            log.warn("deleteEvent: event_id={} not found in aquarium_id={}", eventId, aquariumId);
            return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, eventId));
        }
        aquariumEventRepository.delete(existing.get());
        log.debug("deleteEvent: deleted event_id={} for aquarium_id={}", eventId, aquariumId);
        return new ResultTo<>(null, Message.info(TankMessageCodes.REMOVAL_SUCCEEDED));
    }
}


