/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.MaintenanceMapper;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.model.WaterChangeTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.WaterChangeEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.TankMaintenanceProfileRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.persistence.repositories.WaterChangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implements ownership-safe maintenance operations: regular water exchange interval, recorded
 * water change events and the activated carbon setting. Logging is PII-safe: only the
 * aquarium ID is logged, never the caller's email.
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceServiceImpl.class);

    private final TankMaintenanceProfileRepository profileRepository;
    private final WaterChangeRepository waterChangeRepository;
    private final AquariumRepository aquariumRepository;
    private final UserRepository userRepository;
    private final MaintenanceMapper maintenanceMapper;

    public MaintenanceServiceImpl(TankMaintenanceProfileRepository profileRepository,
                                  WaterChangeRepository waterChangeRepository,
                                  AquariumRepository aquariumRepository,
                                  UserRepository userRepository,
                                  MaintenanceMapper maintenanceMapper) {
        this.profileRepository = profileRepository;
        this.waterChangeRepository = waterChangeRepository;
        this.aquariumRepository = aquariumRepository;
        this.userRepository = userRepository;
        this.maintenanceMapper = maintenanceMapper;
    }

    private AquariumEntity resolveOwnedAquarium(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return null;
        }
        return aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
    }

    private void logRejectedAccess(Long aquariumId) {
        log.warn("Rejected maintenance access on aquarium_id={}", aquariumId);
    }

    @Override
    public ResultTo<MaintenanceTo> getMaintenanceForTank(Long aquariumId, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        MaintenanceTo maintenance = new MaintenanceTo();
        Optional<TankMaintenanceProfileEntity> profile = profileRepository.findByAquariumId(aquariumId);
        maintenance.setProfile(profile
                .map(maintenanceMapper::mapProfileEntityToTo)
                .orElseGet(MaintenanceProfileTo::new));
        maintenance.setWaterChanges(
                maintenanceMapper.mapEntitiesToTos(waterChangeRepository.findByAquariumIdOrderByChangedOnDesc(aquariumId)));
        return new ResultTo<>(maintenance, Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<MaintenanceProfileTo> updateWaterChangeInterval(Long aquariumId, WaterChangeIntervalTo intervalTo,
                                                                    String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        TankMaintenanceProfileEntity entity = loadOrCreateProfile(aquariumId);
        maintenanceMapper.mergeIntervalIntoEntity(intervalTo, entity);
        TankMaintenanceProfileEntity saved = profileRepository.save(entity);
        return new ResultTo<>(maintenanceMapper.mapProfileEntityToTo(saved), Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<MaintenanceProfileTo> updateActivatedCarbon(Long aquariumId, ActivatedCarbonTo carbonTo,
                                                                String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        TankMaintenanceProfileEntity entity = loadOrCreateProfile(aquariumId);
        maintenanceMapper.mergeCarbonIntoEntity(carbonTo, entity);
        if (!Boolean.TRUE.equals(carbonTo.getEnabled())) {
            // FR-011: disabling carbon deterministically clears mode and replacement interval,
            // even if the client still sent values for them.
            entity.setCarbonMode(null);
            entity.setCarbonReplacementIntervalDays(null);
        }
        TankMaintenanceProfileEntity saved = profileRepository.save(entity);
        return new ResultTo<>(maintenanceMapper.mapProfileEntityToTo(saved), Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<WaterChangeTo> createWaterChange(Long aquariumId, WaterChangeTo waterChangeTo, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        WaterChangeEntity entity = maintenanceMapper.mapToToEntity(waterChangeTo);
        entity.setId(null);
        entity.setAquariumId(aquariumId); // always taken from the path, never from the body
        normalize(entity);
        // saveAndFlush (not save): EclipseLink defers the IDENTITY insert to flush time, so with
        // plain save() the generated id is still null when we map the entity to the response TO.
        // The contract (maintenance-api.md) requires the 201 body to carry the id.
        WaterChangeEntity saved = waterChangeRepository.saveAndFlush(entity);
        return new ResultTo<>(maintenanceMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.CREATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<WaterChangeTo> updateWaterChange(Long aquariumId, Long recordId, WaterChangeTo waterChangeTo,
                                                     String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        Optional<WaterChangeEntity> existing = waterChangeRepository.findByIdAndAquariumId(recordId, aquariumId);
        if (existing.isEmpty()) {
            logRejectedAccess(aquariumId);
            return ownershipError(recordId);
        }

        WaterChangeEntity entity = existing.get();
        maintenanceMapper.mergeToIntoEntity(waterChangeTo, entity);
        normalize(entity);
        WaterChangeEntity saved = waterChangeRepository.save(entity);
        return new ResultTo<>(maintenanceMapper.mapEntityToTo(saved), Message.info(TankMessageCodes.UPDATE_SUCCEEDED));
    }

    @Override
    @Transactional
    public ResultTo<WaterChangeTo> deleteWaterChange(Long aquariumId, Long recordId, String userEmail) {
        if (resolveOwnedAquarium(aquariumId, userEmail) == null) {
            logRejectedAccess(aquariumId);
            return ownershipError(aquariumId);
        }

        Optional<WaterChangeEntity> existing = waterChangeRepository.findByIdAndAquariumId(recordId, aquariumId);
        if (existing.isEmpty()) {
            logRejectedAccess(aquariumId);
            return ownershipError(recordId);
        }

        waterChangeRepository.delete(existing.get());
        return new ResultTo<>(null, Message.info(TankMessageCodes.REMOVAL_SUCCEEDED));
    }

    private TankMaintenanceProfileEntity loadOrCreateProfile(Long aquariumId) {
        return profileRepository.findByAquariumId(aquariumId)
                .orElseGet(() -> {
                    TankMaintenanceProfileEntity entity = new TankMaintenanceProfileEntity();
                    entity.setAquariumId(aquariumId);
                    return entity;
                });
    }

    private <T> ResultTo<T> ownershipError(Long id) {
        return new ResultTo<>(null, Message.error(TankMessageCodes.NOT_YOUR_TANK, id));
    }

    private void normalize(WaterChangeEntity entity) {
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
