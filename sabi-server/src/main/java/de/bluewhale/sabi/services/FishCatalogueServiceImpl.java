/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.FishCatalogueMapper;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueI18nTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.FishCatalogueStatus;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueI18nRepository;
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

/**
 * Implementation of {@link FishCatalogueService}.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class FishCatalogueServiceImpl implements FishCatalogueService {

    @Autowired
    private FishCatalogueEntryRepository fishCatalogueEntryRepository;

    @Autowired
    private FishCatalogueI18nRepository fishCatalogueI18nRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FishCatalogueMapper fishCatalogueMapper;

    /** Comma-separated list of admin emails. Configurable via sabi.admin.users property. */
    @Value("${sabi.admin.users:admin@sabi-project.net}")
    private String adminUsers;

    // -----------------------------------------------------------------------

    @Override
    public List<FishCatalogueSearchResultTo> search(String query, String languageCode, String userEmail) {
        if (query == null || query.length() < 2) {
            return new ArrayList<>();
        }

        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : -1L;

        List<FishCatalogueEntryEntity> entries =
                fishCatalogueEntryRepository.searchByQueryAndLang(query, languageCode, userId);

        List<FishCatalogueSearchResultTo> results = new ArrayList<>();
        for (FishCatalogueEntryEntity entry : entries) {
            results.add(fishCatalogueMapper.mapEntity2SearchResult(entry, languageCode));
        }
        return results;
    }

    @Override
    public FishCatalogueEntryTo getEntry(Long id, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        Optional<FishCatalogueEntryEntity> opt = fishCatalogueEntryRepository.findById(id);
        if (opt.isEmpty()) return null;

        FishCatalogueEntryEntity entity = opt.get();
        // Visibility: PUBLIC or own PENDING
        if ("PUBLIC".equals(entity.getStatus())) {
            return fishCatalogueMapper.mapEntity2To(entity);
        }
        if ("PENDING".equals(entity.getStatus()) && user != null
                && user.getId().equals(entity.getProposerUserId())) {
            return fishCatalogueMapper.mapEntity2To(entity);
        }
        return null;
    }

    @Override
    public boolean isDuplicateScientificName(String scientificName) {
        return fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(
                scientificName, Arrays.asList("PENDING", "PUBLIC"));
    }

    @Override
    @Transactional
    public ResultTo<FishCatalogueEntryTo> proposeEntry(FishCatalogueEntryTo entryTo, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, userEmail));
        }

        FishCatalogueEntryEntity entity = fishCatalogueMapper.mapTo2Entity(entryTo);
        entity.setStatus("PENDING");
        entity.setProposerUserId(user.getId());
        entity.setProposalDate(LocalDate.now());

        // Two-step save: parent first (to get ID), then add i18n entries with catalogueId
        List<FishCatalogueI18nEntity> pendingI18n = new ArrayList<>(entity.getI18nEntries());
        entity.getI18nEntries().clear();
        FishCatalogueEntryEntity saved = fishCatalogueEntryRepository.saveAndFlush(entity);

        // Set catalogueId on each i18n entry and re-add to parent
        for (FishCatalogueI18nEntity i18n : pendingI18n) {
            i18n.setCatalogueId(saved.getId());
            i18n.setCatalogueEntry(saved);
        }
        saved.getI18nEntries().addAll(pendingI18n);
        saved = fishCatalogueEntryRepository.saveAndFlush(saved);
        final Long savedId = saved.getId();
        FishCatalogueEntryTo savedTo = fishCatalogueMapper.mapEntity2To(saved);

        // FR-015: Non-blocking duplicate warning
        boolean isDuplicate = isDuplicateScientificName(entryTo.getScientificName())
                && !savedId.equals(
                fishCatalogueEntryRepository.findById(savedId).map(FishCatalogueEntryEntity::getId).orElse(-1L));

        if (isDuplicateScientificName(entryTo.getScientificName())) {
            // Count entries with same name excluding the one just saved
            long count = fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(
                    entryTo.getScientificName(), Arrays.asList("PENDING", "PUBLIC")) ? 1L : 0L;
            // Only warn if there's another entry (not just the one we just created)
            List<FishCatalogueEntryEntity> existing = fishCatalogueEntryRepository
                    .searchByQueryAndLang(entryTo.getScientificName(), "en", user.getId());
            long othersCount = existing.stream()
                    .filter(e -> !e.getId().equals(savedId))
                    .count();
            if (othersCount > 0) {
                return new ResultTo<>(savedTo,
                        Message.warning(FishCatalogueMessageCodes.CATALOGUE_DUPLICATE_WARNING,
                                entryTo.getScientificName()));
            }
        }

        return new ResultTo<>(savedTo,
                Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_PROPOSED, savedId));
    }

    @Override
    @Transactional
    public ResultTo<FishCatalogueEntryTo> approveEntry(Long id, FishCatalogueEntryTo editedEntry, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, adminEmail));
        }

        Optional<FishCatalogueEntryEntity> opt = fishCatalogueEntryRepository.findById(id);
        if (opt.isEmpty()) {
            return new ResultTo<>(null,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, id));
        }

        FishCatalogueEntryEntity entity = opt.get();

        // Merge admin edits before approving
        if (editedEntry != null) {
            if (editedEntry.getScientificName() != null) {
                entity.setScientificName(editedEntry.getScientificName());
            }
            mergeI18nEntries(entity, editedEntry.getI18nEntries());
        }

        entity.setStatus("PUBLIC");
        FishCatalogueEntryEntity saved = fishCatalogueEntryRepository.save(entity);
        return new ResultTo<>(fishCatalogueMapper.mapEntity2To(saved),
                Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_APPROVED, id));
    }

    @Override
    @Transactional
    public ResultTo<FishCatalogueEntryTo> rejectEntry(Long id, String reason, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, adminEmail));
        }

        Optional<FishCatalogueEntryEntity> opt = fishCatalogueEntryRepository.findById(id);
        if (opt.isEmpty()) {
            return new ResultTo<>(null,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, id));
        }

        FishCatalogueEntryEntity entity = opt.get();
        entity.setStatus("REJECTED");
        FishCatalogueEntryEntity saved = fishCatalogueEntryRepository.save(entity);
        return new ResultTo<>(fishCatalogueMapper.mapEntity2To(saved),
                Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_REJECTED, id));
    }

    @Override
    public List<FishCatalogueEntryTo> listPendingProposals(String adminEmail) {
        if (!isAdmin(adminEmail)) return new ArrayList<>();
        List<FishCatalogueEntryEntity> entities =
                fishCatalogueEntryRepository.findAllByStatusOrderByProposalDateAsc("PENDING");
        List<FishCatalogueEntryTo> result = new ArrayList<>();
        for (FishCatalogueEntryEntity entity : entities) {
            result.add(fishCatalogueMapper.mapEntity2To(entity));
        }
        return result;
    }

    @Override
    @Transactional
    public ResultTo<FishCatalogueEntryTo> updateEntry(FishCatalogueEntryTo entryTo, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, userEmail));
        }

        Optional<FishCatalogueEntryEntity> opt = fishCatalogueEntryRepository.findById(entryTo.getId());
        if (opt.isEmpty()) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_FOUND, entryTo.getId()));
        }

        FishCatalogueEntryEntity entity = opt.get();

        // FR-019: REJECTED → read-only
        if ("REJECTED".equals(entity.getStatus())) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_REJECTED_READ_ONLY, entryTo.getId()));
        }

        boolean isAdmin = isAdmin(userEmail);
        boolean isCreator = user.getId().equals(entity.getProposerUserId());

        // PENDING: only creator; PUBLIC: creator + Admin
        if ("PENDING".equals(entity.getStatus()) && !isCreator) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_YOURS, entryTo.getId()));
        }
        if ("PUBLIC".equals(entity.getStatus()) && !isCreator && !isAdmin) {
            return new ResultTo<>(entryTo,
                    Message.error(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_YOURS, entryTo.getId()));
        }

        // Update fields
        if (entryTo.getScientificName() != null) {
            entity.setScientificName(entryTo.getScientificName());
        }
        mergeI18nEntries(entity, entryTo.getI18nEntries());

        FishCatalogueEntryEntity saved = fishCatalogueEntryRepository.save(entity);
        FishCatalogueEntryTo savedTo = fishCatalogueMapper.mapEntity2To(saved);

        // FR-015: re-check duplicate warning if scientific name changed
        if (entryTo.getScientificName() != null
                && isDuplicateScientificName(entryTo.getScientificName())) {
            List<FishCatalogueEntryEntity> existing = fishCatalogueEntryRepository
                    .searchByQueryAndLang(entryTo.getScientificName(), "en", user.getId());
            long othersCount = existing.stream()
                    .filter(e -> !e.getId().equals(saved.getId()))
                    .count();
            if (othersCount > 0) {
                return new ResultTo<>(savedTo,
                        Message.warning(FishCatalogueMessageCodes.CATALOGUE_DUPLICATE_WARNING,
                                entryTo.getScientificName()));
            }
        }

        return new ResultTo<>(savedTo,
                Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_UPDATED, saved.getId()));
    }

    // ---- Helpers ----

    private boolean isAdmin(String userEmail) {
        if (userEmail == null || adminUsers == null) return false;
        return Arrays.stream(adminUsers.split(","))
                .map(String::trim)
                .anyMatch(a -> a.equalsIgnoreCase(userEmail));
    }

    private void syncI18nCatalogueIds(FishCatalogueEntryEntity entity) {
        if (entity.getI18nEntries() != null) {
            entity.getI18nEntries().forEach(i18n -> i18n.setCatalogueEntry(entity));
        }
    }

    private void mergeI18nEntries(FishCatalogueEntryEntity entity, List<FishCatalogueI18nTo> newEntries) {
        if (newEntries == null || newEntries.isEmpty()) return;
        for (FishCatalogueI18nTo newI18n : newEntries) {
            Optional<FishCatalogueI18nEntity> existing = entity.getI18nEntries().stream()
                    .filter(e -> newI18n.getLanguageCode().equals(e.getLanguageCode()))
                    .findFirst();
            if (existing.isPresent()) {
                FishCatalogueI18nEntity e = existing.get();
                e.setCommonName(newI18n.getCommonName());
                e.setDescription(newI18n.getDescription());
                e.setReferenceUrl(newI18n.getReferenceUrl());
            } else {
                FishCatalogueI18nEntity newEntity = fishCatalogueMapper.mapI18nTo2Entity(newI18n);
                newEntity.setCatalogueId(entity.getId());
                newEntity.setCatalogueEntry(entity);
                entity.getI18nEntries().add(newEntity);
            }
        }
    }
}

