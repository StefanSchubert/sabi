/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.FishCatalogueMapper;
import de.bluewhale.sabi.mapper.FishStockMapper;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishPhotoEntity;
import de.bluewhale.sabi.persistence.model.TankFishStockEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.FishPhotoRepository;
import de.bluewhale.sabi.persistence.repositories.TankFishStockRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link FishStockService}.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class FishStockServiceImpl implements FishStockService {

    @Autowired
    private TankFishStockRepository tankFishStockRepository;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FishCatalogueEntryRepository fishCatalogueEntryRepository;

    @Autowired
    private FishPhotoRepository fishPhotoRepository;

    @Autowired
    private FishStockMapper fishStockMapper;

    @Autowired
    private FishCatalogueMapper fishCatalogueMapper;

    @Autowired
    private PhotoStorageService photoStorageService;

    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> addFishToTank(FishStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }

        // Aquarium-Ownership prüfen
        Optional<AquariumEntity> aquarium = aquariumRepository.findById(entry.getAquariumId());
        if (aquarium.isEmpty() || !aquarium.get().getUser().getId().equals(user.getId())) {
            return new ResultTo<>(entry, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, entry.getAquariumId()));
        }

        TankFishStockEntity entity = fishStockMapper.mapTo2Entity(entry);
        entity.setUser(user);

        // FR-009: Wenn Catalogue-Link gesetzt → Scientific Name und referenceUrl snapshooten
        if (entry.getFishCatalogueId() != null) {
            fishCatalogueEntryRepository.findById(entry.getFishCatalogueId()).ifPresent(cat -> {
                entity.setScientificName(cat.getScientificName());
                // referenceUrl snapshot from first EN i18n entry (best effort)
                if (cat.getI18nEntries() != null) {
                    cat.getI18nEntries().stream()
                            .filter(i -> "en".equals(i.getLanguageCode()) && i.getReferenceUrl() != null)
                            .findFirst()
                            .ifPresent(i -> entity.setExternalRefUrl(i.getReferenceUrl()));
                }
            });
        }

        // saveAndFlush() forces EclipseLink to execute the INSERT immediately so that
        // the IDENTITY-generated ID is populated in the returned entity before we map it.
        TankFishStockEntity saved = tankFishStockRepository.saveAndFlush(entity);
        FishStockEntryTo savedTo = fishStockMapper.mapEntity2To(saved);
        return new ResultTo<>(savedTo, Message.info(FishStockMessageCodes.FISH_CREATED, saved.getId()));
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> updateFishEntry(FishStockEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }

        Optional<TankFishStockEntity> existing = tankFishStockRepository.findByIdAndUserId(entry.getId(), user.getId());
        if (existing.isEmpty()) {
            return new ResultTo<>(entry, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, entry.getId()));
        }

        TankFishStockEntity entity = existing.get();
        entity.setCommonName(entry.getCommonName());
        entity.setNickname(entry.getNickname());
        entity.setScientificName(entry.getScientificName());
        entity.setExternalRefUrl(entry.getExternalRefUrl());
        entity.setAddedOn(entry.getAddedOn());
        entity.setObservedBehavior(entry.getObservedBehavior());
        entity.setFishCatalogueId(entry.getFishCatalogueId());

        TankFishStockEntity saved = tankFishStockRepository.save(entity);
        FishStockEntryTo savedTo = fishStockMapper.mapEntity2To(saved);
        return new ResultTo<>(savedTo, Message.info(FishStockMessageCodes.FISH_UPDATED, saved.getId()));
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> recordDeparture(Long fishId, FishDepartureRecordTo departureRecord, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }

        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, fishId));
        }

        TankFishStockEntity entity = fishOpt.get();

        // FR-006: departureDate darf nicht vor addedOn liegen
        if (departureRecord.getDepartureDate().isBefore(entity.getAddedOn())) {
            return new ResultTo<>(fishStockMapper.mapEntity2To(entity),
                    Message.error(FishStockMessageCodes.FISH_DEPARTURE_RECORDED, fishId));
        }

        entity.setExodusOn(departureRecord.getDepartureDate());
        entity.setDepartureReason(departureRecord.getDepartureReason().name());
        TankFishStockEntity saved = tankFishStockRepository.save(entity);
        return new ResultTo<>(fishStockMapper.mapEntity2To(saved),
                Message.info(FishStockMessageCodes.FISH_DEPARTURE_RECORDED, saved.getId()));
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> deletePhysically(Long fishId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }

        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, fishId));
        }

        // FR-024: Wenn departure-Record vorhanden → kein physisches Löschen
        if (tankFishStockRepository.existsByIdAndExodusOnIsNotNull(fishId)) {
            return new ResultTo<>(fishStockMapper.mapEntity2To(fishOpt.get()),
                    Message.error(FishStockMessageCodes.FISH_HAS_DEPARTURE_RECORD, fishId));
        }

        // Foto aufräumen falls vorhanden
        fishPhotoRepository.findByFishId(fishId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            fishPhotoRepository.delete(photo);
        });

        tankFishStockRepository.delete(fishOpt.get());
        return new ResultTo<>(null, Message.info(FishStockMessageCodes.FISH_DELETED, fishId));
    }

    @Override
    public FishStockEntryTo getFishById(Long fishId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        return tankFishStockRepository.findByIdAndUserId(fishId, user.getId())
                .map(entity -> {
                    FishStockEntryTo to = fishStockMapper.mapEntity2To(entity);
                    to.setHasPhoto(fishPhotoRepository.findByFishId(entity.getId()).isPresent());
                    return to;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> deletePhoto(Long fishId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }
        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, fishId));
        }
        fishPhotoRepository.findByFishId(fishId).ifPresent(photo -> {
            photoStorageService.delete(photo.getFilePath());
            fishPhotoRepository.delete(photo);
        });
        FishStockEntryTo to = fishStockMapper.mapEntity2To(fishOpt.get());
        to.setHasPhoto(false);
        return new ResultTo<>(to, Message.info(FishStockMessageCodes.FISH_DELETED, fishId));
    }

    @Override
    public List<FishStockEntryTo> getFishForTank(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return new ArrayList<>();

        // @SQLRestriction filters soft-deleted automatically; findAllByAquariumId returns ALL (active + departed)
        List<TankFishStockEntity> entities = tankFishStockRepository.findAllByAquariumId(aquariumId);
        List<FishStockEntryTo> result = new ArrayList<>();
        for (TankFishStockEntity entity : entities) {
            // Ownership check: only return if the aquarium belongs to the requesting user
            if (!aquariumBelongsToUser(entity.getAquariumId(), user.getId())) {
                continue;
            }
            FishStockEntryTo to = fishStockMapper.mapEntity2To(entity);
            // set hasPhoto
            to.setHasPhoto(fishPhotoRepository.findByFishId(entity.getId()).isPresent());
            result.add(to);
        }
        return result;
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> removeCatalogueLink(Long fishId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }
        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, fishId));
        }
        TankFishStockEntity entity = fishOpt.get();
        entity.setFishCatalogueId(null);
        TankFishStockEntity saved = tankFishStockRepository.save(entity);
        return new ResultTo<>(fishStockMapper.mapEntity2To(saved),
                Message.info(FishStockMessageCodes.CATALOGUE_LINK_REMOVED, fishId));
    }

    @Override
    @Transactional
    public ResultTo<FishStockEntryTo> uploadPhoto(Long fishId, byte[] bytes, String contentType, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_FOUND, userEmail));
        }
        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) {
            return new ResultTo<>(null, Message.error(FishStockMessageCodes.FISH_NOT_YOURS, fishId));
        }

        String relativePath = photoStorageService.store(user.getId(), fishId, bytes, contentType);

        // Upsert FishPhotoEntity
        Optional<FishPhotoEntity> existing = fishPhotoRepository.findByFishId(fishId);
        FishPhotoEntity photoEntity = existing.orElseGet(() -> {
            FishPhotoEntity p = new FishPhotoEntity();
            p.setFishId(fishId);
            return p;
        });
        photoEntity.setFilePath(relativePath);
        photoEntity.setContentType(contentType);
        photoEntity.setFileSize((long) bytes.length);
        photoEntity.setUploadDate(LocalDate.now());
        fishPhotoRepository.save(photoEntity);

        FishStockEntryTo to = fishStockMapper.mapEntity2To(fishOpt.get());
        to.setHasPhoto(true);
        return new ResultTo<>(to, Message.info(FishStockMessageCodes.FISH_PHOTO_UPLOADED, fishId));
    }

    @Override
    public byte[] getPhotoBytes(Long fishId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return new byte[0];

        Optional<TankFishStockEntity> fishOpt = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
        if (fishOpt.isEmpty()) return new byte[0];

        return fishPhotoRepository.findByFishId(fishId)
                .map(photo -> photoStorageService.load(photo.getFilePath()))
                .orElse(new byte[0]);
    }

    // ---- Helpers ----

    private boolean aquariumBelongsToUser(Long aquariumId, Long userId) {
        return aquariumRepository.findById(aquariumId)
                .map(a -> a.getUser().getId().equals(userId))
                .orElse(false);
    }
}



