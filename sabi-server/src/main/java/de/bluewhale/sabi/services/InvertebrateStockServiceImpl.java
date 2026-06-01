/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.InvertebrateStockMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link InvertebrateStockService}.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class InvertebrateStockServiceImpl implements InvertebrateStockService {

    @Autowired
    private TankInvertebrateStockRepository invertebrateStockRepository;

    @Autowired
    private InvertebrateWaterSensitivityRepository waterSensitivityRepository;

    @Autowired
    private InvertebratePhotoRepository photoRepository;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvertebrateStockMapper mapper;

    @Autowired
    @Qualifier("invertebratePhotoStorage")
    private PhotoStorageService photoStorageService;

    // -------------------------------------------------------------------------
    // Core CRUD
    // -------------------------------------------------------------------------

    @Override
    public List<InvertebrateStockEntryTo> listForAquarium(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return Collections.emptyList();
        List<TankInvertebrateStockEntity> entities =
                invertebrateStockRepository.findAllByAquariumIdAndUserId(aquariumId, user.getId());
        return entities.stream()
                .map(e -> {
                    InvertebrateStockEntryTo to = mapper.toTo(e);
                    to.setHasPhoto(photoRepository.findByInvertebrateStockId(e.getId()).isPresent());
                    return to;
                })
                .collect(Collectors.toList());
    }

    @Override
    public InvertebrateStockEntryTo getById(Long invertebrateId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        return invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId())
                .map(e -> {
                    InvertebrateStockEntryTo to = mapper.toTo(e);
                    to.setHasPhoto(photoRepository.findByInvertebrateStockId(e.getId()).isPresent());
                    return to;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> create(InvertebrateStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        // Aquarium ownership check
        Optional<AquariumEntity> aquariumOpt = aquariumRepository.findById(entry.getAquariumId());
        if (aquariumOpt.isEmpty() || !aquariumOpt.get().getUser().getId().equals(user.getId())) {
            return new ResultTo<>(entry, Message.error(InvertebrateStockMessageCodes.AQUARIUM_NOT_YOURS));
        }

        // Marine (sea water) only
        AquariumEntity aquarium = aquariumOpt.get();
        if (aquarium.getWaterType() != WaterType.SEA_WATER) {
            return new ResultTo<>(entry, Message.error(InvertebrateStockMessageCodes.MARINE_ONLY));
        }

        TankInvertebrateStockEntity entity = mapper.toEntity(entry);
        entity.setUser(user);
        TankInvertebrateStockEntity saved = invertebrateStockRepository.saveAndFlush(entity);

        // Persist water sensitivities
        persistWaterSensitivities(saved.getId(), entry.getWaterSensitivityUnitIds());

        log.info("Invertebrate added to tank for user_id={}, invertebrateId={}", user.getId(), saved.getId());
        return new ResultTo<>(mapper.toTo(saved), Message.info(InvertebrateStockMessageCodes.INVERT_CREATED));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> update(InvertebrateStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(entry.getId(), user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(entry, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        TankInvertebrateStockEntity entity = entityOpt.get();
        entity.setSpeciesName(entry.getSpeciesName());
        entity.setScientificName(entry.getScientificName());
        entity.setTaxonomicCategory(entry.getTaxonomicCategory() != null ? entry.getTaxonomicCategory().name() : null);
        entity.setCareLevel(entry.getCareLevel() != null ? entry.getCareLevel().name() : null);
        entity.setMobility(entry.getMobility() != null ? entry.getMobility().name() : null);
        entity.setEcologicalRoles(entry.getEcologicalRoles() != null
                ? entry.getEcologicalRoles().stream()
                        .map(InvertebrateEcologicalRole::name)
                        .collect(Collectors.toList())
                : new ArrayList<>());
        entity.setActivityPattern(entry.getActivityPattern() != null ? entry.getActivityPattern().name() : null);
        entity.setExternalRefUrl(entry.getExternalRefUrl());
        entity.setNotes(entry.getNotes());
        entity.setAddedOn(entry.getAddedOn());
        entity.setInvertebrateCatalogueId(entry.getInvertebrateCatalogueId());

        TankInvertebrateStockEntity saved = invertebrateStockRepository.save(entity);

        // Persist water sensitivities
        persistWaterSensitivities(saved.getId(), entry.getWaterSensitivityUnitIds());

        log.debug("Invertebrate updated, user_id={}, invertebrateId={}", user.getId(), saved.getId());
        return new ResultTo<>(mapper.toTo(saved), Message.info(InvertebrateStockMessageCodes.INVERT_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> delete(Long invertebrateId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        TankInvertebrateStockEntity entity = entityOpt.get();
        if (entity.getDepartedOn() != null) {
            return new ResultTo<>(mapper.toTo(entity),
                    Message.error(InvertebrateStockMessageCodes.INVERT_HAS_DEPARTURE_RECORD));
        }

        // Delete associated photo file
        photoRepository.findByInvertebrateStockId(invertebrateId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            photoRepository.delete(photo);
        });

        invertebrateStockRepository.delete(entity);
        log.info("Invertebrate deleted, user_id={}, invertebrateId={}", user.getId(), invertebrateId);
        return new ResultTo<>(null, Message.info(InvertebrateStockMessageCodes.INVERT_DELETED));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> recordDeparture(Long invertebrateId,
                                                               InvertebrateDepartureRecordTo record,
                                                               String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        TankInvertebrateStockEntity entity = entityOpt.get();

        // Departure date must be >= added_on
        if (record.getDepartedOn().isBefore(entity.getAddedOn())) {
            return new ResultTo<>(mapper.toTo(entity),
                    Message.error(InvertebrateStockMessageCodes.DEPARTURE_DATE_BEFORE_ENTRY));
        }

        entity.setDepartedOn(record.getDepartedOn());
        entity.setDepartureReason(record.getDepartureReason() != null ? record.getDepartureReason().name() : null);
        entity.setDepartureNote(record.getDepartureNote());
        TankInvertebrateStockEntity saved = invertebrateStockRepository.save(entity);
        return new ResultTo<>(mapper.toTo(saved), Message.info(InvertebrateStockMessageCodes.INVERT_DEPARTURE_RECORDED));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> removeCatalogueLink(Long invertebrateId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        TankInvertebrateStockEntity entity = entityOpt.get();
        entity.setInvertebrateCatalogueId(null);
        TankInvertebrateStockEntity saved = invertebrateStockRepository.save(entity);
        return new ResultTo<>(mapper.toTo(saved), Message.info(InvertebrateStockMessageCodes.CATALOGUE_LINK_REMOVED));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> uploadPhoto(Long invertebrateId, MultipartFile file, String userEmail) throws IOException {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        TankInvertebrateStockEntity entity = entityOpt.get();

        String filePath = photoStorageService.store(user.getId(), invertebrateId, file.getBytes(), file.getContentType());

        // Upsert: update existing photo record or create new one (avoids unique-key violation on re-upload)
        InvertebratePhotoEntity photoEntity = photoRepository.findByInvertebrateStockId(invertebrateId)
                .orElseGet(() -> {
                    InvertebratePhotoEntity p = new InvertebratePhotoEntity();
                    p.setInvertebrateStockId(invertebrateId);
                    return p;
                });
        photoEntity.setFilePath(filePath);
        photoEntity.setContentType(file.getContentType());
        photoEntity.setUploadDate(LocalDate.now());
        photoRepository.save(photoEntity);

        log.info("Photo uploaded for invertebrate {}, user_id={}", invertebrateId, user.getId());
        return new ResultTo<>(mapper.toTo(entity), Message.info(InvertebrateStockMessageCodes.INVERT_PHOTO_UPLOADED));
    }

    @Override
    public byte[] getPhotoBytes(Long invertebrateId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return new byte[0];
        if (invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId()).isEmpty()) return new byte[0];
        return photoRepository.findByInvertebrateStockId(invertebrateId)
                .map(photo -> photoStorageService.load(photo.getFilePath()))
                .orElse(new byte[0]);
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateStockEntryTo> deletePhoto(Long invertebrateId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_FOUND));
        }

        Optional<TankInvertebrateStockEntity> entityOpt =
                invertebrateStockRepository.findByIdAndUserId(invertebrateId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(InvertebrateStockMessageCodes.INVERT_NOT_OWNER));
        }

        photoRepository.findByInvertebrateStockId(invertebrateId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            photoRepository.delete(photo);
        });

        return new ResultTo<>(mapper.toTo(entityOpt.get()), Message.info(InvertebrateStockMessageCodes.INVERT_UPDATED));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void persistWaterSensitivities(Long invertebrateStockId, List<Integer> unitIds) {
        waterSensitivityRepository.deleteAllByInvertebrateStockId(invertebrateStockId);
        waterSensitivityRepository.flush();
        if (unitIds != null && !unitIds.isEmpty()) {
            List<InvertebrateWaterSensitivityEntity> entities = mapper.toWaterSensitivityEntities(invertebrateStockId, unitIds);
            waterSensitivityRepository.saveAll(entities);
        }
    }

    // -------------------------------------------------------------------------
    // Public report integration (US7)
    // -------------------------------------------------------------------------

    @Override
    public List<InvertebrateStockEntryTo> getActiveInvertebratesForReport(Long aquariumId) {
        return invertebrateStockRepository.findActiveByAquariumId(aquariumId)
                .stream()
                .map(e -> {
                    InvertebrateStockEntryTo to = mapper.toTo(e);
                    to.setHasPhoto(photoRepository.findByInvertebrateStockId(e.getId()).isPresent());
                    return to;
                })
                .collect(Collectors.toList());
    }
}
