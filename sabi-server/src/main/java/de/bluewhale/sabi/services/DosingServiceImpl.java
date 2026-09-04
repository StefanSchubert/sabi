/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.DosingMapper;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.DosingEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.DosingRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implements ownership-safe CRUD operations for separate dosing records.
 */
@Service
@Transactional(readOnly = true)
public class DosingServiceImpl implements DosingService {

    private final DosingRepository dosingRepository;
    private final AquariumRepository aquariumRepository;
    private final UserRepository userRepository;
    private final DosingMapper dosingMapper;

    public DosingServiceImpl(DosingRepository dosingRepository, AquariumRepository aquariumRepository,
                             UserRepository userRepository, DosingMapper dosingMapper) {
        this.dosingRepository = dosingRepository;
        this.aquariumRepository = aquariumRepository;
        this.userRepository = userRepository;
        this.dosingMapper = dosingMapper;
    }

    private AquariumEntity resolveOwnedAquarium(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return null;
        }
        return aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
    }

    @Override
    public List<DosingTo> listDosingsForTank(Long aquariumId, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            return Collections.emptyList();
        }
        return dosingMapper.mapEntitiesToTos(dosingRepository.findByAquariumIdOrderByRecordedOnDesc(aquariumId));
    }

    @Override
    @Transactional
    public ResultTo<DosingTo> createDosing(Long aquariumId, DosingTo dosingTo, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            return ownershipError(aquariumId);
        }

        DosingEntity entity = dosingMapper.mapToToEntity(dosingTo);
        entity.setId(null);
        entity.setAquariumId(aquariumId);
        normalize(entity);
        DosingEntity saved = dosingRepository.save(entity);
        return new ResultTo<>(dosingMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.CREATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<DosingTo> updateDosing(Long aquariumId, Long dosingId, DosingTo dosingTo, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            return ownershipError(aquariumId);
        }

        Optional<DosingEntity> existing = dosingRepository.findByIdAndAquariumId(dosingId, aquariumId);
        if (existing.isEmpty()) {
            return ownershipError(dosingId);
        }

        DosingEntity entity = existing.get();
        dosingMapper.mergeToIntoEntity(dosingTo, entity);
        normalize(entity);
        DosingEntity saved = dosingRepository.save(entity);
        return new ResultTo<>(dosingMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<DosingTo> deleteDosing(Long aquariumId, Long dosingId, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            return ownershipError(aquariumId);
        }

        Optional<DosingEntity> existing = dosingRepository.findByIdAndAquariumId(dosingId, aquariumId);
        if (existing.isEmpty()) {
            return ownershipError(dosingId);
        }

        dosingRepository.delete(existing.get());
        return new ResultTo<>(null, Message.info(TankMessageCodes.REMOVAL_SUCCEEDED));
    }

    private ResultTo<DosingTo> ownershipError(Long id) {
        return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, id));
    }

    private void normalize(DosingEntity entity) {
        entity.setProductName(trimToNull(entity.getProductName()));
        entity.setAmountUnit(trimToNull(entity.getAmountUnit()));
        entity.setCategory(trimToNull(entity.getCategory()));
        entity.setDosingInterval(trimToNull(entity.getDosingInterval()));
        entity.setDosingMethod(trimToNull(entity.getDosingMethod()));
        entity.setSolutionDescription(trimToNull(entity.getSolutionDescription()));
        entity.setNote(trimToNull(entity.getNote()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
