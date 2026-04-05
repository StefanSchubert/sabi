/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link ReefDataExportService}.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class ReefDataExportServiceImpl implements ReefDataExportService {

    private static final String EXPORT_LANG = "en";
    private static final String META_DESCRIPTION =
            "Sabi reef data export \u2014 all aquariums, measurements, plague records, fish, corals, and treatments for use with AI chatbot consultations.";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private PlagueRecordEntityRepository plagueRecordRepository;

    @Autowired
    private FishRepository fishRepository;

    @Autowired
    private CoralRepository coralRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private LocalizedUnitRepository localizedUnitRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private LocalizedPlagueRepository localizedPlagueRepository;

    @Autowired
    private LocalizedPlagueStatusRepository localizedPlagueStatusRepository;

    @Autowired
    private FishCatalogueRepository fishCatalogueRepository;

    @Autowired
    private CoralCatalogueRepository coralCatalogueRepository;

    @Autowired
    private RemedyRepository remedyRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public ReefDataExportTo buildExportForUser(String userEmail) {
        // Step 1: resolve email -> userId (needed for audit log hash)
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user.getId();

        // Step 2: anonymised audit log (FR-014) — hash only, never email/username
        log.info("DATA_EXPORT userId={}", sha256Hex(userId.toString()));

        // Step 3: assemble metadata
        ExportMetaTo meta = new ExportMetaTo();
        meta.setExportedAt(Instant.now().toString());
        meta.setSabiSchemaVersion(ReefDataExportTo.SCHEMA_VERSION);
        meta.setDescription(META_DESCRIPTION);

        // Step 4: load all active aquariums for user and build nested export objects
        List<AquariumEntity> aquariums = aquariumRepository.findAllByUser_IdIs(userId);
        List<AquariumExportTo> aquariumExports = new ArrayList<>();
        for (AquariumEntity aquarium : aquariums) {
            aquariumExports.add(buildAquariumExport(aquarium));
        }

        // Step 5: assemble top-level document
        ReefDataExportTo exportTo = new ReefDataExportTo();
        exportTo.setMeta(meta);
        exportTo.setAquariums(aquariumExports);
        return exportTo;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Internal utilities
    // -------------------------------------------------------------------------

    /**
     * Computes a lowercase hex-encoded SHA-256 hash of the given input string.
     * Uses Java's built-in {@code java.security.MessageDigest} — no external library required.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in every Java SE implementation
            throw new IllegalStateException("SHA-256 MessageDigest not available", e);
        }
    }

    private AquariumExportTo buildAquariumExport(AquariumEntity aquarium) {        AquariumExportTo ato = new AquariumExportTo();
        ato.setId(aquarium.getId());
        ato.setDescription(aquarium.getDescription());
        ato.setWaterType(aquarium.getWaterType() != null ? aquarium.getWaterType().name() : null);
        ato.setSize(aquarium.getSize());
        ato.setSizeUnit(aquarium.getSizeUnit() != null ? aquarium.getSizeUnit().name() : null);
        ato.setActive(aquarium.getActive());
        if (aquarium.getInceptionDate() != null) {
            ato.setInceptionDate(new SimpleDateFormat("yyyy-MM-dd").format(aquarium.getInceptionDate()));
        }

        ato.setMeasurements(buildMeasurementExports(aquarium.getId()));
        ato.setPlagueRecords(buildPlagueRecordExports(aquarium.getId()));
        ato.setFish(buildFishExports(aquarium.getId()));
        ato.setCorals(buildCoralExports(aquarium.getId()));
        ato.setTreatments(buildTreatmentExports(aquarium.getId()));

        return ato;
    }

    private List<MeasurementExportTo> buildMeasurementExports(Long aquariumId) {
        List<MeasurementEntity> entities = measurementRepository.findByAquarium_Id(aquariumId);
        List<MeasurementExportTo> result = new ArrayList<>();
        for (MeasurementEntity m : entities) {
            MeasurementExportTo mto = new MeasurementExportTo();
            mto.setMeasuredOn(m.getMeasuredOn() != null ? m.getMeasuredOn().toString() : null);
            mto.setMeasuredValue(m.getMeasuredValue());
            mto.setUnitId(m.getUnitId());

            // T013: unit name resolution
            String unitSign = null;
            String unitName = null;
            boolean resolved = false;
            if (m.getUnitId() != null) {
                Optional<UnitEntity> unitOpt = unitRepository.findAll().stream()
                        .filter(u -> u.getId().equals(m.getUnitId()))
                        .findFirst();
                if (unitOpt.isPresent()) {
                    unitSign = unitOpt.get().getName();
                }
                LocalizedUnitEntity localizedUnit = localizedUnitRepository.findByLanguageAndUnitId(EXPORT_LANG, m.getUnitId());
                if (localizedUnit != null) {
                    unitName = localizedUnit.getDescription();
                    resolved = true;
                }
            }
            mto.setUnitSign(unitSign);
            mto.setUnitName(unitName);
            mto.setUnitNameResolved(resolved);

            result.add(mto);
        }
        return result;
    }

    private List<PlagueRecordExportTo> buildPlagueRecordExports(Long aquariumId) {
        List<PlagueRecordEntity> entities = plagueRecordRepository.findByAquarium_Id(aquariumId);
        List<PlagueRecordExportTo> result = new ArrayList<>();
        for (PlagueRecordEntity p : entities) {
            PlagueRecordExportTo pto = new PlagueRecordExportTo();
            pto.setObservedOn(p.getObservedOn() != null ? p.getObservedOn().toString() : null);
            pto.setPlagueId(p.getPlagueId());
            pto.setPlagueIntervallId(p.getPlagueIntervallId());

            // T014: plague name resolution
            String plagueName = null;
            boolean plagueNameResolved = false;
            if (p.getPlagueId() != null) {
                LocalizedPlagueEntity lp = localizedPlagueRepository.findByLanguageAndPlagueId(EXPORT_LANG, p.getPlagueId());
                if (lp != null) {
                    plagueName = lp.getCommonName();
                    plagueNameResolved = true;
                }
            }
            pto.setPlagueName(plagueName);
            pto.setPlagueNameResolved(plagueNameResolved);

            // T014: plague status resolution (field is observedPlagueStatus in entity)
            Integer plagueStatusId = p.getObservedPlagueStatus();
            pto.setPlagueStatusId(plagueStatusId);
            String plagueStatusName = null;
            boolean plagueStatusResolved = false;
            if (plagueStatusId != null) {
                LocalizedPlagueStatusEntity lps = localizedPlagueStatusRepository.findByLanguageAndPlagueStatusId(EXPORT_LANG, plagueStatusId);
                if (lps != null) {
                    plagueStatusName = lps.getDescription();
                    plagueStatusResolved = true;
                }
            }
            pto.setPlagueStatusName(plagueStatusName);
            pto.setPlagueStatusResolved(plagueStatusResolved);

            result.add(pto);
        }
        return result;
    }

    private List<FishExportTo> buildFishExports(Long aquariumId) {
        List<FishEntity> entities = fishRepository.findFishEntitiesByAquariumId(aquariumId);
        List<FishExportTo> result = new ArrayList<>();
        for (FishEntity f : entities) {
            FishExportTo fto = new FishExportTo();
            fto.setFishCatalogueId(f.getFishCatalogueId());
            fto.setAddedOn(f.getAddedOn() != null ? f.getAddedOn().toString() : null);
            fto.setObservedBehavior(f.getObservedBehavior());

            // T015: fish catalogue resolution
            String scientificName = null;
            if (f.getFishCatalogueId() != null) {
                Optional<FishCatalogueEntity> catOpt = fishCatalogueRepository.findById(f.getFishCatalogueId());
                scientificName = catOpt.map(FishCatalogueEntity::getScientificName).orElse(null);
            }
            fto.setScientificName(scientificName);

            result.add(fto);
        }
        return result;
    }

    private List<CoralExportTo> buildCoralExports(Long aquariumId) {
        List<CoralEntity> entities = coralRepository.findCoralEntitiesByAquariumId(aquariumId);
        List<CoralExportTo> result = new ArrayList<>();
        for (CoralEntity c : entities) {
            CoralExportTo cto = new CoralExportTo();
            // Note: source field is coralCatalougeId (typo preserved from entity)
            cto.setCoralCatalogueId(c.getCoralCatalougeId());
            cto.setObservedBehavior(c.getObservedBehavior());

            // T015: coral catalogue resolution
            String scientificName = null;
            Optional<CoralCatalogueEntity> catOpt = coralCatalogueRepository.findById(c.getCoralCatalougeId());
            scientificName = catOpt.map(CoralCatalogueEntity::getScientificName).orElse(null);
            cto.setScientificName(scientificName);

            result.add(cto);
        }
        return result;
    }

    private List<TreatmentExportTo> buildTreatmentExports(Long aquariumId) {
        List<TreatmentEntity> entities = treatmentRepository.findTreatmentEntitiesByAquariumId(aquariumId);
        List<TreatmentExportTo> result = new ArrayList<>();
        for (TreatmentEntity t : entities) {
            TreatmentExportTo tto = new TreatmentExportTo();
            tto.setGivenOn(t.getGivenOn() != null ? t.getGivenOn().toString() : null);
            tto.setAmount(t.getAmount());
            tto.setUnitId(t.getUnitId());
            tto.setRemedyId(t.getRemedyId());
            tto.setDescription(t.getDescription());

            // Unit sign for treatment (same resolution as measurements)
            String unitSign = null;
            if (t.getUnitId() != null) {
                Optional<UnitEntity> unitOpt = unitRepository.findAll().stream()
                        .filter(u -> u.getId().equals(t.getUnitId()))
                        .findFirst();
                if (unitOpt.isPresent()) {
                    unitSign = unitOpt.get().getName();
                }
            }
            tto.setUnitSign(unitSign);

            String unitName = null;
            if (t.getUnitId() != null) {
                Optional<LocalizedUnitEntity> localizedUnitOpt =
                        localizedUnitRepository.findByLanguageAndUnitId("en", t.getUnitId());
                if (localizedUnitOpt.isPresent()) {
                    unitName = localizedUnitOpt.get().getName();
                }
            }
            tto.setUnitName(unitName);
            // T016: remedy resolution
            String productName = null;
            String vendor = null;
            if (t.getRemedyId() != null) {
                Optional<RemedyEntity> remedyOpt = remedyRepository.findById(t.getRemedyId());
                if (remedyOpt.isPresent()) {
                    productName = remedyOpt.get().getProductname();
                    vendor = remedyOpt.get().getVendor();
                }
            }
            tto.setProductName(productName);
            tto.setVendor(vendor);

            result.add(tto);
        }
        return result;
    }
}
