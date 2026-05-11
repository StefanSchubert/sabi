/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.mapper.AquariumMapper;
import de.bluewhale.sabi.mapper.FishStockMapper;
import de.bluewhale.sabi.mapper.MeasurementMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.PublicReportLinkEntity;
import de.bluewhale.sabi.persistence.model.TankFishStockEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PublicReportService}.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class PublicReportServiceImpl implements PublicReportService {

    @Autowired
    private PublicReportLinkRepository publicReportLinkRepository;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private AquariumPhotoRepository aquariumPhotoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TankFishStockRepository tankFishStockRepository;

    @Autowired
    private FishPhotoRepository fishPhotoRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private LocalizedUnitRepository localizedUnitRepository;

    @Autowired
    private AquariumMapper aquariumMapper;

    @Autowired
    private FishStockMapper fishStockMapper;

    @Autowired
    private MeasurementMapper measurementMapper;

    @Autowired
    @Qualifier("aquariumPhotoStorage")
    private PhotoStorageService aquariumPhotoStorage;

    @Autowired
    @Qualifier("fishPhotoStorage")
    private PhotoStorageService fishPhotoStorage;

    // -----------------------------------------------------------------------

    @Override
    public PublicReportLinkTo getLinkForTank(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        // ownership check
        AquariumEntity aq = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
        if (aq == null) return null;
        return publicReportLinkRepository.findByAquariumId(aquariumId)
                .map(this::toTo)
                .orElse(null);
    }

    @Override
    @Transactional
    public PublicReportLinkTo createOrReplaceLink(Long aquariumId, LocalDateTime validUntil, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return null;
        // ownership check
        AquariumEntity aq = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
        if (aq == null) return null;

        // Replace existing link or create new
        PublicReportLinkEntity entity = publicReportLinkRepository.findByAquariumId(aquariumId)
                .orElseGet(PublicReportLinkEntity::new);
        entity.setAquariumId(aquariumId);
        entity.setLinkToken(UUID.randomUUID().toString());
        entity.setValidUntil(validUntil);

        PublicReportLinkEntity saved = publicReportLinkRepository.saveAndFlush(entity);
        log.info("Created/replaced public report link for aquarium {} by user_id={}", aquariumId, user.getId());
        return toTo(saved);
    }

    @Override
    @Transactional
    public void deleteLink(Long aquariumId, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) return;
        // ownership check
        AquariumEntity aq = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
        if (aq == null) return;
        publicReportLinkRepository.deleteByAquariumId(aquariumId);
        log.info("Deleted public report link for aquarium {} by user_id={}", aquariumId, user.getId());
    }

    @Override
    public PublicReefReportTo getReport(String linkToken, String language) {
        PublicReefReportTo report = new PublicReefReportTo();
        report.setReportGeneratedAt(LocalDateTime.now());

        Optional<PublicReportLinkEntity> linkOpt = publicReportLinkRepository.findByLinkToken(linkToken);
        if (linkOpt.isEmpty()) {
            report.setLinkExpired(true);
            return report;
        }

        PublicReportLinkEntity link = linkOpt.get();

        // Check expiry
        if (link.getValidUntil() != null && link.getValidUntil().isBefore(LocalDateTime.now())) {
            report.setLinkExpired(true);
            return report;
        }

        report.setLinkExpired(false);

        // Load aquarium
        Optional<AquariumEntity> aqOpt = aquariumRepository.findById(link.getAquariumId());
        if (aqOpt.isEmpty()) {
            report.setLinkExpired(true);
            return report;
        }
        AquariumEntity aq = aqOpt.get();
        AquariumTo aquariumTo = aquariumMapper.mapAquariumEntity2To(aq);
        // Mask sensitive fields before publishing
        aquariumTo.setTemperatureApiKey(null);
        aquariumTo.setUserId(null);
        aquariumTo.setHasPhoto(aquariumPhotoRepository.findByAquariumId(aq.getId()).isPresent());
        report.setTank(aquariumTo);

        // Owner username (anonymised - only the username, not the email)
        UserEntity owner = aq.getUser();
        report.setOwnerUsername(owner != null ? owner.getUsername() : "Unknown");

        // Current fish inhabitants (no departure)
        List<TankFishStockEntity> allFish = tankFishStockRepository.findAllByAquariumId(link.getAquariumId());
        List<FishStockEntryTo> inhabitants = allFish.stream()
                .filter(f -> f.getExodusOn() == null)
                .map(f -> {
                    FishStockEntryTo to = fishStockMapper.mapEntity2To(f);
                    to.setHasPhoto(fishPhotoRepository.findByFishId(f.getId()).isPresent());
                    return to;
                })
                .collect(Collectors.toList());
        report.setInhabitants(inhabitants);

        // Measurements: last 3 months, grouped by unitId
        LocalDateTime since = LocalDateTime.now().minusMonths(3);
        List<de.bluewhale.sabi.persistence.model.MeasurementEntity> recentMeasurements =
                measurementRepository.findByAquarium_Id(link.getAquariumId()).stream()
                        .filter(m -> m.getMeasuredOn() != null && m.getMeasuredOn().isAfter(since))
                        .sorted(Comparator.comparing(de.bluewhale.sabi.persistence.model.MeasurementEntity::getMeasuredOn))
                        .collect(Collectors.toList());

        Map<Integer, List<MeasurementTo>> measurementsByUnit = new LinkedHashMap<>();
        Map<Integer, UnitTo> unitMap = new LinkedHashMap<>();

        for (de.bluewhale.sabi.persistence.model.MeasurementEntity m : recentMeasurements) {
            MeasurementTo mTo = measurementMapper.mapMeasurementEntity2To(m);
            measurementsByUnit.computeIfAbsent(m.getUnitId(), k -> new ArrayList<>()).add(mTo);
        }

        // Resolve unit meta-data for all units present in measurements
        for (Integer unitId : measurementsByUnit.keySet()) {
            unitRepository.findById(unitId).ifPresent(unitEntity -> {
                UnitTo unitTo = new UnitTo();
                unitTo.setId(unitEntity.getId());
                unitTo.setUnitSign(unitEntity.getName());
                de.bluewhale.sabi.persistence.model.LocalizedUnitEntity localizedUnit =
                        localizedUnitRepository.findByLanguageAndUnitId(language, unitId);
                unitTo.setDescription(localizedUnit != null ? localizedUnit.getDescription() : unitEntity.getName());
                unitMap.put(unitId, unitTo);
            });
        }

        report.setMeasurementsByUnit(measurementsByUnit);
        report.setUnitMap(unitMap);

        return report;
    }

    // ---- Helpers ----

    private PublicReportLinkTo toTo(PublicReportLinkEntity entity) {
        PublicReportLinkTo to = new PublicReportLinkTo();
        to.setId(entity.getId());
        to.setAquariumId(entity.getAquariumId());
        to.setLinkToken(entity.getLinkToken());
        to.setValidUntil(entity.getValidUntil());
        return to;
    }

    @Override
    public byte[] getAquariumPhotoBytes(String linkToken) {
        Optional<PublicReportLinkEntity> linkOpt = publicReportLinkRepository.findByLinkToken(linkToken);
        if (linkOpt.isEmpty()) return new byte[0];
        PublicReportLinkEntity link = linkOpt.get();
        if (link.getValidUntil() != null && link.getValidUntil().isBefore(LocalDateTime.now())) {
            return new byte[0];
        }
        return aquariumPhotoRepository.findByAquariumId(link.getAquariumId())
                .map(photo -> aquariumPhotoStorage.load(photo.getFilePath()))
                .orElse(new byte[0]);
    }

    @Override
    public byte[] getFishPhotoBytes(String linkToken, Long fishId) {
        Optional<PublicReportLinkEntity> linkOpt = publicReportLinkRepository.findByLinkToken(linkToken);
        if (linkOpt.isEmpty()) return new byte[0];
        PublicReportLinkEntity link = linkOpt.get();
        if (link.getValidUntil() != null && link.getValidUntil().isBefore(LocalDateTime.now())) {
            return new byte[0];
        }
        // Verify the fish belongs to the aquarium referenced by the link
        boolean fishBelongsToAquarium = tankFishStockRepository.findById(fishId)
                .map(f -> link.getAquariumId().equals(f.getAquariumId()))
                .orElseGet(() -> {
                    log.warn("Public report photo requested for non-existent fishId={} with token={}", fishId, linkToken);
                    return false;
                });
        if (!fishBelongsToAquarium) return new byte[0];

        return fishPhotoRepository.findByFishId(fishId)
                .map(photo -> fishPhotoStorage.load(photo.getFilePath()))
                .orElse(new byte[0]);
    }
}