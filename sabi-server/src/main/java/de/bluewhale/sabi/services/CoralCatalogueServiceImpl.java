/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.CoralCatalogueMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.CoralCatalogueEntity;
import de.bluewhale.sabi.persistence.model.CoralCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.CoralCatalogueI18nRepository;
import de.bluewhale.sabi.persistence.repositories.CoralCatalogueRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CoralCatalogueService}.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CoralCatalogueServiceImpl implements CoralCatalogueService {

    @Autowired
    private CoralCatalogueRepository coralCatalogueRepository;

    @Autowired
    private CoralCatalogueI18nRepository coralCatalogueI18nRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoralCatalogueMapper coralCatalogueMapper;

    @Autowired
    private NotificationService notificationService;

    /** Comma-separated list of admin emails. Same as fish catalogue configuration. */
    @Value("${sabi.admin.users:admin@sabi-project.net}")
    private String adminUsers;

    // -----------------------------------------------------------------------

    @Override
    public List<CoralCatalogueSearchResultTo> search(String query, String lang, String userEmail) {
        if (query == null || query.trim().length() < 2) return new ArrayList<>();

        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;

        // Load public entries + own pending entries
        List<CoralCatalogueEntity> entries = coralCatalogueRepository.findAll().stream()
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .filter(e -> matchesQuery(e, query, lang))
                .collect(Collectors.toList());

        return entries.stream()
                .map(e -> coralCatalogueMapper.toSearchResult(e, lang))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(CoralCatalogueEntity e, String query, String lang) {
        String q = query.toLowerCase();
        if (e.getScientificName() != null && e.getScientificName().toLowerCase().contains(q)) return true;
        if (e.getI18nEntries() != null) {
            return e.getI18nEntries().stream().anyMatch(i18n ->
                    i18n.getCommonName() != null && i18n.getCommonName().toLowerCase().contains(q));
        }
        return false;
    }

    @Override
    public List<CoralCatalogueEntryTo> listAll(String userEmail, String lang) {
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;

        return coralCatalogueRepository.findAll().stream()
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .map(coralCatalogueMapper::toTo)
                .collect(Collectors.toList());
    }

    @Override
    public CoralCatalogueEntryTo getById(Long id, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;

        return coralCatalogueRepository.findById(id)
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .map(coralCatalogueMapper::toTo)
                .orElse(null);
    }

    @Override
    @Transactional
    public ResultTo<CoralCatalogueEntryTo> proposeEntry(CoralCatalogueEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(CoralCatalogueMessageCodes.UNKNOWN_USER));
        }

        // FR-025: duplicate name check (non-blocking warning)
        List<CoralCatalogueEntity> duplicates = coralCatalogueRepository
                .findByScientificNameAndStatusIn(entry.getScientificName(),
                        List.of("PENDING", "PUBLIC"));
        boolean hasDuplicate = !duplicates.isEmpty();

        CoralCatalogueEntity entity = coralCatalogueMapper.toEntity(entry);
        entity.setStatus("PENDING");
        entity.setProposerUserId(user.getId());
        entity.setProposalDate(LocalDate.now());

        // Two-step save: parent first (to get ID), then add i18n entries with catalogueId set
        CoralCatalogueEntity saved = coralCatalogueRepository.saveAndFlush(entity);

        if (entry.getI18nEntries() != null) {
            List<CoralCatalogueI18nEntity> i18nEntities = new ArrayList<>();
            for (CoralCatalogueI18nTo i18nTo : entry.getI18nEntries()) {
                CoralCatalogueI18nEntity i18nEntity = new CoralCatalogueI18nEntity();
                i18nEntity.setLanguageCode(i18nTo.getLanguageCode());
                i18nEntity.setCommonName(i18nTo.getCommonName());
                i18nEntity.setDescription(i18nTo.getDescription());
                i18nEntity.setReferenceUrl(i18nTo.getReferenceUrl());
                i18nEntity.setCatalogueId(saved.getId());
                i18nEntity.setCatalogue(saved);
                i18nEntities.add(i18nEntity);
            }
            saved.setI18nEntries(i18nEntities);
            saved = coralCatalogueRepository.saveAndFlush(saved);
        }

        log.info("Coral catalogue entry proposed by user_id={}, id={}", user.getId(), saved.getId());

        // Notify admins asynchronously — failure must not block the user's proposal
        try {
            notificationService.sendCatalogueProposalNotification("Coral", saved.getScientificName());
        } catch (Exception e) {
            log.warn("Failed to send admin notification for coral proposal id={}", saved.getId(), e);
        }

        Message msg = hasDuplicate
                ? Message.warning(CoralCatalogueMessageCodes.DUPLICATE_SCIENTIFIC_NAME_WARNING)
                : Message.info(CoralCatalogueMessageCodes.ENTRY_PROPOSED);

        return new ResultTo<>(coralCatalogueMapper.toTo(saved), msg);
    }

    @Override
    @Transactional
    public ResultTo<CoralCatalogueEntryTo> updateEntry(CoralCatalogueEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(CoralCatalogueMessageCodes.UNKNOWN_USER));
        }

        Optional<CoralCatalogueEntity> entityOpt = coralCatalogueRepository.findById(entry.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(entry, Message.error(CoralCatalogueMessageCodes.ENTRY_NOT_FOUND));
        }

        CoralCatalogueEntity entity = entityOpt.get();
        boolean isAdmin = isAdmin(userEmail);
        boolean isCreator = user.getId().equals(entity.getProposerUserId());

        // FR-029: REJECTED entries read-only for non-admin
        if ("REJECTED".equals(entity.getStatus()) && !isAdmin) {
            return new ResultTo<>(entry, Message.error(CoralCatalogueMessageCodes.READ_ONLY_REJECTED));
        }

        if (!isCreator && !isAdmin) {
            return new ResultTo<>(entry, Message.error(CoralCatalogueMessageCodes.NOT_YOUR_ENTRY));
        }

        // FR-029: re-evaluate duplicate check on name change
        boolean hasDuplicate = false;
        if (!entity.getScientificName().equals(entry.getScientificName())) {
            List<CoralCatalogueEntity> duplicates = coralCatalogueRepository
                    .findByScientificNameAndStatusIn(entry.getScientificName(), List.of("PENDING", "PUBLIC"));
            hasDuplicate = duplicates.stream().anyMatch(d -> !d.getId().equals(entity.getId()));
        }

        entity.setScientificName(entry.getScientificName());
        entity.setClassification(entry.getClassification() != null ? entry.getClassification().name() : null);
        entity.setCareLevel(entry.getCareLevel() != null ? entry.getCareLevel().name() : null);

        // Upsert i18n entries
        if (entry.getI18nEntries() != null) {
            for (CoralCatalogueI18nTo i18nTo : entry.getI18nEntries()) {
                Optional<CoralCatalogueI18nEntity> existing =
                        coralCatalogueI18nRepository.findByCatalogueIdAndLanguageCode(entity.getId(), i18nTo.getLanguageCode());
                CoralCatalogueI18nEntity i18nEntity = existing.orElseGet(() -> {
                    CoralCatalogueI18nEntity n = new CoralCatalogueI18nEntity();
                    n.setCatalogueId(entity.getId());
                    n.setLanguageCode(i18nTo.getLanguageCode());
                    n.setCatalogue(entity);
                    return n;
                });
                i18nEntity.setCommonName(i18nTo.getCommonName());
                i18nEntity.setDescription(i18nTo.getDescription());
                i18nEntity.setReferenceUrl(i18nTo.getReferenceUrl());
                coralCatalogueI18nRepository.save(i18nEntity);
            }
        }

        CoralCatalogueEntity saved = coralCatalogueRepository.save(entity);

        Message msg = hasDuplicate
                ? Message.warning(CoralCatalogueMessageCodes.DUPLICATE_SCIENTIFIC_NAME_WARNING)
                : Message.info(CoralCatalogueMessageCodes.ENTRY_UPDATED);

        return new ResultTo<>(coralCatalogueMapper.toTo(saved), msg);
    }

    // ---- Admin methods ----

    @Transactional
    public ResultTo<CoralCatalogueEntryTo> approveEntry(Long id, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(CoralCatalogueMessageCodes.NOT_ADMIN));
        }
        return coralCatalogueRepository.findById(id)
                .map(entity -> {
                    entity.setStatus("PUBLIC");
                    CoralCatalogueEntity saved = coralCatalogueRepository.save(entity);
                    log.info("Coral catalogue entry {} approved by admin", id);
                    return new ResultTo<>(coralCatalogueMapper.toTo(saved),
                            Message.info(CoralCatalogueMessageCodes.ENTRY_APPROVED));
                })
                .orElse(new ResultTo<>(null, Message.error(CoralCatalogueMessageCodes.ENTRY_NOT_FOUND)));
    }

    @Transactional
    public ResultTo<CoralCatalogueEntryTo> rejectEntry(Long id, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(CoralCatalogueMessageCodes.NOT_ADMIN));
        }
        return coralCatalogueRepository.findById(id)
                .map(entity -> {
                    entity.setStatus("REJECTED");
                    CoralCatalogueEntity saved = coralCatalogueRepository.save(entity);
                    log.info("Coral catalogue entry {} rejected by admin", id);
                    return new ResultTo<>(coralCatalogueMapper.toTo(saved),
                            Message.info(CoralCatalogueMessageCodes.ENTRY_REJECTED));
                })
                .orElse(new ResultTo<>(null, Message.error(CoralCatalogueMessageCodes.ENTRY_NOT_FOUND)));
    }

    @Transactional
    public ResultTo<CoralCatalogueEntryTo> adminUpdateEntry(Long id, CoralCatalogueEntryTo entry, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(CoralCatalogueMessageCodes.NOT_ADMIN));
        }
        entry.setId(id);
        return updateEntry(entry, adminEmail);
    }

    public List<CoralCatalogueEntryTo> listPending(String adminEmail) {
        if (!isAdmin(adminEmail)) return new ArrayList<>();
        return coralCatalogueRepository.findByStatusOrderByProposalDateDesc("PENDING")
                .stream().map(coralCatalogueMapper::toTo).collect(Collectors.toList());
    }

    /** List ALL coral catalogue entries regardless of status (admin view). */
    public List<CoralCatalogueEntryTo> listAllForAdmin(String adminEmail) {
        if (!isAdmin(adminEmail)) return new ArrayList<>();
        return coralCatalogueRepository.findAll().stream()
                .sorted((a, b) -> {
                    String sa = a.getScientificName() != null ? a.getScientificName() : "";
                    String sb = b.getScientificName() != null ? b.getScientificName() : "";
                    return sa.compareToIgnoreCase(sb);
                })
                .map(coralCatalogueMapper::toTo)
                .collect(Collectors.toList());
    }

    // ---- Helpers ----

    private boolean isAdmin(String userEmail) {
        if (userEmail == null || adminUsers == null) return false;
        return Arrays.stream(adminUsers.split(","))
                .map(String::trim)
                .anyMatch(a -> a.equalsIgnoreCase(userEmail));
    }
}

