/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.PublicReefReportTo;
import de.bluewhale.sabi.model.PublicReportLinkTo;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API gateway for public HouseReef report share links.
 *
 * @author Stefan Schubert
 */
public interface PublicReportService extends Serializable {

    /**
     * Fetch the active share link for the given aquarium.
     *
     * @param aquariumId           ID of the aquarium
     * @param jwtBackendAuthToken  auth token
     * @return link data or null if none exists
     * @throws BusinessException on network error
     */
    PublicReportLinkTo getLinkForTank(@NotNull Long aquariumId, @NotNull String jwtBackendAuthToken) throws BusinessException;

    /**
     * Create or replace the public share link for the given aquarium.
     *
     * @param aquariumId           ID of the aquarium
     * @param validUntil           optional expiry; null means no expiry
     * @param jwtBackendAuthToken  auth token
     * @return the new link
     * @throws BusinessException on network error
     */
    PublicReportLinkTo createOrReplaceLink(@NotNull Long aquariumId, LocalDateTime validUntil,
                                           @NotNull String jwtBackendAuthToken) throws BusinessException;

    /**
     * Delete the public share link for the given aquarium.
     *
     * @param aquariumId           ID of the aquarium
     * @param jwtBackendAuthToken  auth token
     * @throws BusinessException on network error
     */
    void deleteLink(@NotNull Long aquariumId, @NotNull String jwtBackendAuthToken) throws BusinessException;

    /**
     * Fetch the public report for the given share token (no auth required).
     *
     * @param token share token UUID
     * @param lang  ISO-639-1 language code for unit labels
     * @return report data
     * @throws BusinessException on network error
     */
    @NotNull
    PublicReefReportTo getReport(@NotNull String token, @NotNull String lang) throws BusinessException;

    /**
     * Updates the includeEvents flag for the active report link of the given aquarium.
     *
     * @param aquariumId    tank PK
     * @param includeEvents new flag value
     * @param token         auth token
     * @throws BusinessException on network or authorization error
     */
    void updateIncludeEventsFlag(@NotNull Long aquariumId, boolean includeEvents, @NotNull String token)
            throws BusinessException;
}
