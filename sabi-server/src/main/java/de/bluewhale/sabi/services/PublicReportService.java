/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.PublicReefReportTo;
import de.bluewhale.sabi.model.PublicReportLinkTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service managing public HouseReef report share links and report data assembly.
 *
 * @author Stefan Schubert
 */
public interface PublicReportService {

    /**
     * Returns the active public link for the given aquarium, or null if none exists.
     *
     * @param aquariumId  ID of the aquarium
     * @param userEmail   authenticated user email (ownership check)
     * @return link data or null
     */
    PublicReportLinkTo getLinkForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    /**
     * Creates or replaces the public share link for the given aquarium.
     * Generating a new link immediately invalidates any previous token for this aquarium.
     *
     * @param aquariumId  ID of the aquarium
     * @param validUntil  optional expiry; null means no expiry
     * @param userEmail   authenticated user email (ownership check)
     * @return the newly created link, or null if the aquarium does not belong to the user
     */
    @Transactional
    PublicReportLinkTo createOrReplaceLink(@NotNull Long aquariumId, LocalDateTime validUntil, @NotNull String userEmail);

    /**
     * Removes the public share link for the given aquarium.
     *
     * @param aquariumId  ID of the aquarium
     * @param userEmail   authenticated user email (ownership check)
     */
    @Transactional
    void deleteLink(@NotNull Long aquariumId, @NotNull String userEmail);

    /**
     * Assembles the full public report for the given share token.
     * Returns a report with {@code linkExpired = true} (and empty data) if the token
     * has expired or does not exist.
     *
     * @param linkToken UUID share token from the public URL
     * @param language  ISO-639-1 language code for unit labels
     * @return report data, never null
     */
    @NotNull
    PublicReefReportTo getReport(@NotNull String linkToken, @NotNull String language);

    /**
     * Returns the raw aquarium photo bytes for the given token.
     * Token validation (existence + expiry) is applied; returns empty array if invalid.
     *
     * @param linkToken UUID share token
     * @return photo bytes or empty array
     */
    @NotNull
    byte[] getAquariumPhotoBytes(@NotNull String linkToken);

    /**
     * Returns the raw fish photo bytes for the given token and fish ID.
     * Validates that the token is valid and that the fish belongs to the associated aquarium.
     *
     * @param linkToken UUID share token
     * @param fishId    ID of the fish
     * @return photo bytes or empty array
     */
    @NotNull
    byte[] getFishPhotoBytes(@NotNull String linkToken, @NotNull Long fishId);

    /**
     * Returns the raw coral photo bytes for the given token and coral ID.
     * Validates that the token is valid and that the coral belongs to the associated aquarium.
     *
     * @param linkToken UUID share token
     * @param coralId   ID of the coral stock entry
     * @return photo bytes or empty array
     */
    @NotNull
    byte[] getCoralPhotoBytes(@NotNull String linkToken, @NotNull Long coralId);

    /**
     * Returns the raw invertebrate photo bytes for the given token and invertebrate ID.
     * Validates that the token is valid and that the invertebrate belongs to the associated aquarium.
     *
     * @param linkToken       UUID share token
     * @param invertebrateId  ID of the invertebrate stock entry
     * @return photo bytes or empty array
     */
    @NotNull
    byte[] getInvertebratePhotoBytes(@NotNull String linkToken, @NotNull Long invertebrateId);

    /**
     * Persists the includeEvents flag for an existing report link.
     * Verifies that the aquarium belongs to the user before updating.
     *
     * @param aquariumId    tank PK
     * @param includeEvents new flag value
     * @param userEmail     authenticated user email
     * @return true on success, false if aquarium/link does not exist or belongs to another user
     */
    @Transactional
    boolean updateIncludeEvents(@NotNull Long aquariumId, boolean includeEvents, @NotNull String userEmail);

    /**
     * Persists the includeCorals flag for an existing report link (005-coral-stock).
     *
     * @param aquariumId     tank PK
     * @param includeCorals  new flag value
     * @param userEmail      authenticated user email
     * @return true on success, false if aquarium/link does not exist or belongs to another user
     */
    @Transactional
    boolean updateIncludeCorals(@NotNull Long aquariumId, boolean includeCorals, @NotNull String userEmail);

    /**
     * Toggle whether invertebrate stock is included in the public report for an aquarium.
     *
     * @param aquariumId        ID of the aquarium
     * @param includeInvertebrates  whether to include active invertebrate stock
     * @param userEmail         authenticated user's email
     * @return true if update succeeded, false if link not found or not yours
     */
    @Transactional
    boolean updateIncludeInvertebrates(@NotNull Long aquariumId, boolean includeInvertebrates, @NotNull String userEmail);

    /**
     * Toggle whether fish inhabitants are included in the public report for an aquarium.
     *
     * @param aquariumId  ID of the aquarium
     * @param includeFish whether to include active fish stock
     * @param userEmail   authenticated user's email
     * @return true if update succeeded, false if link not found or not yours
     */
    @Transactional
    boolean updateIncludeFish(@NotNull Long aquariumId, boolean includeFish, @NotNull String userEmail);
}
