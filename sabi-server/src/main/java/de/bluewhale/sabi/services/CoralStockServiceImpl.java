/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.CoralStockMapper;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CoralStockService}.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CoralStockServiceImpl implements CoralStockService {

    @Autowired
    private TankCoralStockRepository coralStockRepository;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoralGrowthHistoryRepository growthRepository;

    @Autowired
    private CoralPolypConditionRepository polypRepository;

    @Autowired
    private CoralPhotoRepository photoRepository;

    @Autowired
    private CoralStockMapper mapper;

    @Autowired
    @Qualifier("coralPhotoStorage")
    private PhotoStorageService photoStorageService;

    // -------------------------------------------------------------------------
    // Core CRUD
    // -------------------------------------------------------------------------

    @Override
    public List<CoralStockEntryTo> getCoralsForTank(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return Collections.emptyList();

        List<TankCoralStockEntity> entities = coralStockRepository.findByAquariumIdAndDeletedAtIsNull(aquariumId);
        return entities.stream()
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(e -> {
                    CoralStockEntryTo to = mapper.toTo(e);
                    to.setHasPhoto(photoRepository.findByCoralStockId(e.getId()).isPresent());
                    return to;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CoralStockEntryTo getCoralById(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        return coralStockRepository.findByIdAndUserId(coralId, user.getId())
                .map(e -> {
                    CoralStockEntryTo to = mapper.toTo(e);
                    to.setHasPhoto(photoRepository.findByCoralStockId(e.getId()).isPresent());
                    return to;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> addCoralToTank(CoralStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        // Aquarium ownership check
        Optional<AquariumEntity> aquariumOpt = aquariumRepository.findById(entry.getAquariumId());
        if (aquariumOpt.isEmpty() || !aquariumOpt.get().getUser().getId().equals(user.getId())) {
            return new ResultTo<>(entry, Message.error(CoralStockMessageCodes.AQUARIUM_NOT_YOURS));
        }

        // C-8: marine (sea water) only
        AquariumEntity aquarium = aquariumOpt.get();
        if (aquarium.getWaterType() != WaterType.SEA_WATER) {
            return new ResultTo<>(entry, Message.error(CoralStockMessageCodes.MARINE_ONLY));
        }

        TankCoralStockEntity entity = mapper.toEntity(entry);
        entity.setUser(user);
        // saveAndFlush() forces EclipseLink to execute the INSERT immediately so that
        // the generated ID is available before the transaction commits (same as Fish)
        TankCoralStockEntity saved = coralStockRepository.saveAndFlush(entity);
        log.info("Coral added to tank for user_id={}, coralId={}", user.getId(), saved.getId());
        return new ResultTo<>(mapper.toTo(saved), Message.info(CoralStockMessageCodes.CORAL_CREATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> updateCoralEntry(CoralStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(entry.getId(), user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(entry, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity entity = entityOpt.get();
        entity.setSpeciesName(entry.getSpeciesName());
        entity.setScientificName(entry.getScientificName());
        entity.setClassification(entry.getClassification() != null ? entry.getClassification().name() : null);
        entity.setCareLevel(entry.getCareLevel() != null ? entry.getCareLevel().name() : null);
        entity.setExternalRefUrl(entry.getExternalRefUrl());
        entity.setNotes(entry.getNotes());
        entity.setAddedOn(entry.getAddedOn());
        entity.setCoralCatalogueId(entry.getCoralCatalogueId());

        TankCoralStockEntity saved = coralStockRepository.save(entity);
        log.debug("Coral updated, user_id={}, coralId={}", user.getId(), saved.getId());
        return new ResultTo<>(mapper.toTo(saved), Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> deletePhysically(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity entity = entityOpt.get();
        // FR-012: cannot delete if departure record exists
        if (entity.getDepartedOn() != null) {
            return new ResultTo<>(mapper.toTo(entity), Message.error(CoralStockMessageCodes.CORAL_HAS_DEPARTURE_RECORD));
        }

        // Delete associated photo file
        photoRepository.findByCoralStockId(coralId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            photoRepository.delete(photo);
        });

        coralStockRepository.delete(entity);
        log.info("Coral deleted, user_id={}, coralId={}", user.getId(), coralId);
        return new ResultTo<>(null, Message.info(CoralStockMessageCodes.CORAL_DELETED));
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> recordDeparture(Long coralId, CoralDepartureRecordTo record, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity entity = entityOpt.get();

        // FR-006: departure date must be >= added_on
        if (record.getDepartureDate().isBefore(entity.getAddedOn())) {
            return new ResultTo<>(mapper.toTo(entity), Message.error(CoralStockMessageCodes.DEPARTURE_DATE_BEFORE_ENTRY));
        }

        entity.setDepartedOn(record.getDepartureDate());
        entity.setDepartureReason(record.getDepartureReason().name());
        entity.setDepartureNote(record.getDepartureNote());

        TankCoralStockEntity saved = coralStockRepository.save(entity);
        log.info("Coral departure recorded, user_id={}, coralId={}", user.getId(), coralId);
        return new ResultTo<>(mapper.toTo(saved), Message.info(CoralStockMessageCodes.CORAL_DEPARTURE_RECORDED));
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> removeCatalogueLink(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity entity = entityOpt.get();
        entity.setCoralCatalogueId(null);
        TankCoralStockEntity saved = coralStockRepository.save(entity);
        return new ResultTo<>(mapper.toTo(saved), Message.info(CoralStockMessageCodes.CATALOGUE_LINK_REMOVED));
    }

    // -------------------------------------------------------------------------
    // Photo management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> uploadPhoto(Long coralId, MultipartFile file, String userEmail) throws IOException {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        byte[] bytes = file.getBytes();
        String contentType = file.getContentType();

        try {
            String relativePath = photoStorageService.store(user.getId(), coralId, bytes, contentType);

            // Upsert CoralPhotoEntity
            CoralPhotoEntity photo = photoRepository.findByCoralStockId(coralId)
                    .orElseGet(() -> {
                        CoralPhotoEntity p = new CoralPhotoEntity();
                        p.setCoralStockId(coralId);
                        return p;
                    });
            photo.setFilePath(relativePath);
            photo.setContentType(contentType);
            photo.setUploadDate(LocalDate.now());
            photoRepository.save(photo);

            log.info("Coral photo uploaded, user_id={}, coralId={}", user.getId(), coralId);
            CoralStockEntryTo to = mapper.toTo(entityOpt.get());
            to.setHasPhoto(true);
            return new ResultTo<>(to, Message.info(CoralStockMessageCodes.CORAL_PHOTO_UPLOADED));
        } catch (PhotoStorageService.FishPhotoTooLargeException e) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.PHOTO_TOO_LARGE));
        } catch (PhotoStorageService.FishPhotoInvalidFormatException e) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.PHOTO_INVALID_FORMAT));
        }
    }

    @Override
    public byte[] getPhotoBytes(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return new byte[0];

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) return new byte[0];

        return photoRepository.findByCoralStockId(coralId)
                .map(photo -> photoStorageService.load(photo.getFilePath()))
                .orElse(new byte[0]);
    }

    @Override
    @Transactional
    public ResultTo<CoralStockEntryTo> deletePhoto(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> entityOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        photoRepository.findByCoralStockId(coralId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            photoRepository.delete(photo);
        });

        CoralStockEntryTo to = mapper.toTo(entityOpt.get());
        to.setHasPhoto(false);
        return new ResultTo<>(to, Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    // -------------------------------------------------------------------------
    // Growth History
    // -------------------------------------------------------------------------

    @Override
    public List<CoralGrowthHistoryTo> getGrowthHistory(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return Collections.emptyList();
        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) return Collections.emptyList();
        return growthRepository.findByCoralStockIdOrderByMeasuredOnDesc(coralId)
                .stream().map(mapper::toGrowthTo).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResultTo<CoralGrowthHistoryTo> addGrowthRecord(Long coralId, CoralGrowthHistoryTo record, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> coralOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (coralOpt.isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity coral = coralOpt.get();

        // FR-015: date must not exceed departure date
        if (coral.getDepartedOn() != null && record.getMeasuredOn().isAfter(coral.getDepartedOn())) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.GROWTH_DATE_AFTER_DEPARTURE));
        }

        // C-9: BRANCH_COUNT must be integer
        if (CoralGrowthType.BRANCH_COUNT == record.getMeasurementType()) {
            if (record.getMeasurementValue().stripTrailingZeros().scale() > 0) {
                return new ResultTo<>(record, Message.error(CoralStockMessageCodes.BRANCH_COUNT_MUST_BE_INTEGER));
            }
        }

        CoralGrowthHistoryEntity entity = mapper.toGrowthEntity(record);
        entity.setCoralStockId(coralId);
        CoralGrowthHistoryEntity saved = growthRepository.save(entity);
        return new ResultTo<>(mapper.toGrowthTo(saved), Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralGrowthHistoryTo> updateGrowthRecord(Long coralId, CoralGrowthHistoryTo record, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        Optional<CoralGrowthHistoryEntity> entityOpt = growthRepository.findByIdAndCoralStockId(record.getId(), coralId);
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        CoralGrowthHistoryEntity entity = entityOpt.get();
        entity.setMeasuredOn(record.getMeasuredOn());
        entity.setMeasurementValue(record.getMeasurementValue());
        // FR-039: measurement_type is IMMUTABLE after creation — do NOT update it

        return new ResultTo<>(mapper.toGrowthTo(growthRepository.save(entity)), Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralGrowthHistoryTo> deleteGrowthRecord(Long coralId, Long recordId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        Optional<CoralGrowthHistoryEntity> entityOpt = growthRepository.findByIdAndCoralStockId(recordId, coralId);
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        growthRepository.delete(entityOpt.get());
        return new ResultTo<>(null, Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    // -------------------------------------------------------------------------
    // Polyp Condition History
    // -------------------------------------------------------------------------

    @Override
    public List<CoralPolypConditionTo> getPolypHistory(Long coralId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return Collections.emptyList();
        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) return Collections.emptyList();
        return polypRepository.findByCoralStockIdOrderByObservedOnDesc(coralId)
                .stream().map(mapper::toPolypTo).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResultTo<CoralPolypConditionTo> addPolypObservation(Long coralId, CoralPolypConditionTo record, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        Optional<TankCoralStockEntity> coralOpt = coralStockRepository.findByIdAndUserId(coralId, user.getId());
        if (coralOpt.isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        TankCoralStockEntity coral = coralOpt.get();

        // FR-019: date must not exceed departure date
        if (coral.getDepartedOn() != null && record.getObservedOn().isAfter(coral.getDepartedOn())) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.POLYP_DATE_AFTER_DEPARTURE));
        }

        CoralPolypConditionEntity entity = mapper.toPolypEntity(record);
        entity.setCoralStockId(coralId);
        CoralPolypConditionEntity saved = polypRepository.save(entity);
        return new ResultTo<>(mapper.toPolypTo(saved), Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralPolypConditionTo> updatePolypObservation(Long coralId, CoralPolypConditionTo record, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        Optional<CoralPolypConditionEntity> entityOpt = polypRepository.findByIdAndCoralStockId(record.getId(), coralId);
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(record, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        CoralPolypConditionEntity entity = entityOpt.get();
        entity.setObservedOn(record.getObservedOn());
        entity.setPolypCondition(record.getCondition() != null ? record.getCondition().name() : null);

        return new ResultTo<>(mapper.toPolypTo(polypRepository.save(entity)), Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    @Override
    @Transactional
    public ResultTo<CoralPolypConditionTo> deletePolypObservation(Long coralId, Long recordId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        if (coralStockRepository.findByIdAndUserId(coralId, user.getId()).isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL));
        }

        Optional<CoralPolypConditionEntity> entityOpt = polypRepository.findByIdAndCoralStockId(recordId, coralId);
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(CoralStockMessageCodes.CORAL_NOT_FOUND));
        }

        polypRepository.delete(entityOpt.get());
        return new ResultTo<>(null, Message.info(CoralStockMessageCodes.CORAL_UPDATED));
    }

    // -------------------------------------------------------------------------
    // Report / Export
    // -------------------------------------------------------------------------

    @Override
    public List<PublicReefReportCoralTo> getActiveCoralsForReport(Long aquariumId) {
        List<TankCoralStockEntity> active = coralStockRepository.findActiveByAquariumId(aquariumId);
        return active.stream().map(coral -> {
            // Latest growth per type
            List<CoralGrowthHistoryEntity> growthHistory =
                    growthRepository.findByCoralStockIdOrderByMeasuredOnDesc(coral.getId());
            Map<String, BigDecimal> latestByType = new LinkedHashMap<>();
            for (CoralGrowthHistoryEntity g : growthHistory) {
                latestByType.putIfAbsent(g.getMeasurementType(), g.getMeasurementValue());
            }

            // Latest polyp condition
            List<CoralPolypConditionEntity> polypHistory =
                    polypRepository.findByCoralStockIdOrderByObservedOnDesc(coral.getId());
            String latestCondition = polypHistory.isEmpty() ? null : polypHistory.get(0).getPolypCondition();

            PublicReefReportCoralTo to = new PublicReefReportCoralTo();
            to.setId(coral.getId());
            to.setHasPhoto(photoRepository.findByCoralStockId(coral.getId()).isPresent());
            to.setSpeciesName(coral.getSpeciesName());
            to.setClassification(coral.getClassification());
            to.setLatestGrowthByType(latestByType);
            to.setLatestPolypCondition(latestCondition);
            return to;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CoralExportTo> getCorralsForExport(Long aquariumId) {
        // Export ALL corals including departed (ignore @SQLRestriction for export)
        // Using a native query bypass approach — load via JPQL ignoring soft-delete
        List<TankCoralStockEntity> all = coralStockRepository.findByAquariumIdAndDeletedAtIsNull(aquariumId);
        return all.stream().map(coral -> {
            List<CoralGrowthHistoryEntity> growth = growthRepository.findByCoralStockIdOrderByMeasuredOnDesc(coral.getId());
            List<CoralPolypConditionEntity> polyp = polypRepository.findByCoralStockIdOrderByObservedOnDesc(coral.getId());
            return mapper.toExportTo(coral, growth, polyp);
        }).collect(Collectors.toList());
    }
}

