/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.InvertebrateCatalogueMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueEntity;
import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.InvertebrateCatalogueI18nRepository;
import de.bluewhale.sabi.persistence.repositories.InvertebrateCatalogueRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for invertebrate catalogue management.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class InvertebrateCatalogueServiceImpl implements InvertebrateCatalogueService {

    @Autowired private InvertebrateCatalogueRepository invertebrateCatalogueRepository;
    @Autowired private InvertebrateCatalogueI18nRepository invertebrateCatalogueI18nRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InvertebrateCatalogueMapper invertebrateCatalogueMapper;
    @Autowired private NotificationService notificationService;

    @Value("${sabi.admin.users:admin@sabi-project.net}")
    private String adminUsers;

    @Override
    public List<InvertebrateCatalogueSearchResultTo> search(String query, String lang, String userEmail) {
        if (query == null || query.trim().length() < 2) return new ArrayList<>();
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;
        List<InvertebrateCatalogueEntity> entries = invertebrateCatalogueRepository.findAll().stream()
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .filter(e -> matchesQuery(e, query))
                .collect(Collectors.toList());
        return entries.stream()
                .map(e -> invertebrateCatalogueMapper.toSearchResult(e, lang))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(InvertebrateCatalogueEntity e, String query) {
        String q = query.toLowerCase();
        if (e.getScientificName() != null && e.getScientificName().toLowerCase().contains(q)) return true;
        if (e.getI18nEntries() != null) {
            return e.getI18nEntries().stream().anyMatch(i18n ->
                    i18n.getCommonName() != null && i18n.getCommonName().toLowerCase().contains(q));
        }
        return false;
    }

    @Override
    public List<InvertebrateCatalogueEntryTo> listAll(String userEmail, String lang) {
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;
        return invertebrateCatalogueRepository.findAll().stream()
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .map(invertebrateCatalogueMapper::toTo)
                .collect(Collectors.toList());
    }

    @Override
    public InvertebrateCatalogueEntryTo getById(Long id, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        Long userId = user != null ? user.getId() : null;
        return invertebrateCatalogueRepository.findById(id)
                .filter(e -> {
                    String status = e.getStatus();
                    if ("PUBLIC".equals(status)) return true;
                    if ("PENDING".equals(status) && userId != null && userId.equals(e.getProposerUserId())) return true;
                    return false;
                })
                .map(invertebrateCatalogueMapper::toTo)
                .orElse(null);
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateCatalogueEntryTo> proposeEntry(InvertebrateCatalogueEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(InvertebrateCatalogueMessageCodes.UNKNOWN_USER));
        }
        List<InvertebrateCatalogueEntity> duplicates = invertebrateCatalogueRepository
                .findByScientificNameAndStatusIn(entry.getScientificName(), Arrays.asList("PENDING", "PUBLIC"));
        boolean hasDuplicate = !duplicates.isEmpty();

        InvertebrateCatalogueEntity entity = invertebrateCatalogueMapper.toEntity(entry);
        entity.setStatus("PENDING");
        entity.setProposerUserId(user.getId());
        entity.setProposalDate(LocalDate.now());

        InvertebrateCatalogueEntity saved = invertebrateCatalogueRepository.saveAndFlush(entity);

        if (entry.getI18nEntries() != null) {
            List<InvertebrateCatalogueI18nEntity> i18nEntities = new ArrayList<>();
            for (InvertebrateCatalogueI18nTo i18nTo : entry.getI18nEntries()) {
                InvertebrateCatalogueI18nEntity i18nEntity = new InvertebrateCatalogueI18nEntity();
                i18nEntity.setLanguageCode(i18nTo.getLanguageCode());
                i18nEntity.setCommonName(i18nTo.getCommonName());
                i18nEntity.setDescription(i18nTo.getDescription());
                i18nEntity.setReferenceUrl(i18nTo.getReferenceUrl());
                i18nEntity.setCatalogueId(saved.getId());
                i18nEntity.setCatalogue(saved);
                i18nEntities.add(i18nEntity);
            }
            saved.setI18nEntries(i18nEntities);
            saved = invertebrateCatalogueRepository.saveAndFlush(saved);
        }

        log.info("Invertebrate catalogue entry proposed by user_id={}, id={}", user.getId(), saved.getId());
        try {
            notificationService.sendCatalogueProposalNotification("Invertebrate", saved.getScientificName());
        } catch (Exception e) {
            log.warn("Failed to send admin notification for invertebrate proposal id={}", saved.getId(), e);
        }

        Message msg = hasDuplicate
                ? Message.warning(InvertebrateCatalogueMessageCodes.CATALOGUE_NAME_DUPLICATE)
                : Message.info(InvertebrateCatalogueMessageCodes.CATALOGUE_PROPOSED);

        return new ResultTo<>(invertebrateCatalogueMapper.toTo(saved), msg);
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateCatalogueEntryTo> updateEntry(InvertebrateCatalogueEntryTo entry, String userEmail) {
        UserEntity user = userRepository.getByEmail(userEmail);
        if (user == null) {
            return new ResultTo<>(entry, Message.error(InvertebrateCatalogueMessageCodes.UNKNOWN_USER));
        }
        Optional<InvertebrateCatalogueEntity> entityOpt = invertebrateCatalogueRepository.findById(entry.getId());
        if (entityOpt.isEmpty()) {
            return new ResultTo<>(entry, Message.error(InvertebrateCatalogueMessageCodes.CATALOGUE_NOT_FOUND));
        }
        InvertebrateCatalogueEntity entity = entityOpt.get();
        boolean isAdmin = isAdmin(userEmail);
        boolean isCreator = user.getId().equals(entity.getProposerUserId());

        if ("REJECTED".equals(entity.getStatus()) && !isAdmin) {
            return new ResultTo<>(entry, Message.error(InvertebrateCatalogueMessageCodes.READ_ONLY_REJECTED));
        }
        if (!isCreator && !isAdmin) {
            return new ResultTo<>(entry, Message.error(InvertebrateCatalogueMessageCodes.NOT_YOUR_ENTRY));
        }

        boolean hasDuplicate = false;
        if (!entity.getScientificName().equals(entry.getScientificName())) {
            List<InvertebrateCatalogueEntity> duplicates = invertebrateCatalogueRepository
                    .findByScientificNameAndStatusIn(entry.getScientificName(), Arrays.asList("PENDING", "PUBLIC"));
            hasDuplicate = duplicates.stream().anyMatch(d -> !d.getId().equals(entity.getId()));
        }

        entity.setScientificName(entry.getScientificName());
        entity.setTaxonomicCategory(entry.getTaxonomicCategory() != null ? entry.getTaxonomicCategory().name() : null);
        entity.setCareLevel(entry.getCareLevel() != null ? entry.getCareLevel().name() : null);

        if (entry.getI18nEntries() != null) {
            for (InvertebrateCatalogueI18nTo i18nTo : entry.getI18nEntries()) {
                Optional<InvertebrateCatalogueI18nEntity> existing =
                        invertebrateCatalogueI18nRepository.findByCatalogueIdAndLanguageCode(entity.getId(), i18nTo.getLanguageCode());
                InvertebrateCatalogueI18nEntity i18nEntity = existing.orElseGet(() -> {
                    InvertebrateCatalogueI18nEntity n = new InvertebrateCatalogueI18nEntity();
                    n.setCatalogueId(entity.getId());
                    n.setLanguageCode(i18nTo.getLanguageCode());
                    n.setCatalogue(entity);
                    return n;
                });
                i18nEntity.setCommonName(i18nTo.getCommonName());
                i18nEntity.setDescription(i18nTo.getDescription());
                i18nEntity.setReferenceUrl(i18nTo.getReferenceUrl());
                invertebrateCatalogueI18nRepository.save(i18nEntity);
            }
        }

        InvertebrateCatalogueEntity saved = invertebrateCatalogueRepository.save(entity);
        Message msg = hasDuplicate
                ? Message.warning(InvertebrateCatalogueMessageCodes.CATALOGUE_NAME_DUPLICATE)
                : Message.info(InvertebrateCatalogueMessageCodes.CATALOGUE_UPDATED);
        return new ResultTo<>(invertebrateCatalogueMapper.toTo(saved), msg);
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateCatalogueEntryTo> approveEntry(Long id, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(InvertebrateCatalogueMessageCodes.NOT_ADMIN));
        }
        return invertebrateCatalogueRepository.findById(id)
                .map(entity -> {
                    entity.setStatus("PUBLIC");
                    InvertebrateCatalogueEntity saved = invertebrateCatalogueRepository.save(entity);
                    log.info("Invertebrate catalogue entry {} approved by admin", id);
                    return new ResultTo<>(invertebrateCatalogueMapper.toTo(saved),
                            Message.info(InvertebrateCatalogueMessageCodes.CATALOGUE_APPROVED));
                })
                .orElse(new ResultTo<>(null, Message.error(InvertebrateCatalogueMessageCodes.CATALOGUE_NOT_FOUND)));
    }

    @Override
    @Transactional
    public ResultTo<InvertebrateCatalogueEntryTo> rejectEntry(Long id, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(InvertebrateCatalogueMessageCodes.NOT_ADMIN));
        }
        return invertebrateCatalogueRepository.findById(id)
                .map(entity -> {
                    entity.setStatus("REJECTED");
                    InvertebrateCatalogueEntity saved = invertebrateCatalogueRepository.save(entity);
                    log.info("Invertebrate catalogue entry {} rejected by admin", id);
                    return new ResultTo<>(invertebrateCatalogueMapper.toTo(saved),
                            Message.info(InvertebrateCatalogueMessageCodes.CATALOGUE_REJECTED));
                })
                .orElse(new ResultTo<>(null, Message.error(InvertebrateCatalogueMessageCodes.CATALOGUE_NOT_FOUND)));
    }

    @Override
    public List<InvertebrateCatalogueEntryTo> listPending(String adminEmail) {
        if (!isAdmin(adminEmail)) return new ArrayList<>();
        return invertebrateCatalogueRepository.findByStatusOrderByProposalDateDesc("PENDING")
                .stream().map(invertebrateCatalogueMapper::toTo).collect(Collectors.toList());
    }

    @Override
    public List<InvertebrateCatalogueEntryTo> listAllForAdmin(String adminEmail) {
        if (!isAdmin(adminEmail)) return new ArrayList<>();
        return invertebrateCatalogueRepository.findAll().stream()
                .sorted((a, b) -> {
                    String sa = a.getScientificName() != null ? a.getScientificName() : "";
                    String sb = b.getScientificName() != null ? b.getScientificName() : "";
                    return sa.compareToIgnoreCase(sb);
                })
                .map(invertebrateCatalogueMapper::toTo)
                .collect(Collectors.toList());
    }

    public ResultTo<InvertebrateCatalogueEntryTo> adminUpdateEntry(Long id, InvertebrateCatalogueEntryTo entry, String adminEmail) {
        if (!isAdmin(adminEmail)) {
            return new ResultTo<>(null, Message.error(InvertebrateCatalogueMessageCodes.NOT_ADMIN));
        }
        entry.setId(id);
        return updateEntry(entry, adminEmail);
    }

    private boolean isAdmin(String userEmail) {
        if (userEmail == null || adminUsers == null) return false;
        return Arrays.stream(adminUsers.split(","))
                .map(String::trim)
                .anyMatch(a -> a.equalsIgnoreCase(userEmail));
    }
}
