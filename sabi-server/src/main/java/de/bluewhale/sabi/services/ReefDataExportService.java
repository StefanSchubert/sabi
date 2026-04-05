/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.ReefDataExportTo;

/**
 * Service for assembling the full reef data export for a single user.
 * All catalogue references are resolved to English for AI chatbot readability.
 *
 * @author Stefan Schubert
 */
public interface ReefDataExportService {

    /**
     * Builds a complete {@link ReefDataExportTo} for the given user.
     *
     * <p>Resolves all catalogue references (unit names, plague names, species names, remedy names)
     * to English. Missing catalogue entries fall back to raw IDs with {@code *Resolved=false}
     * — the record is never skipped (FR-008).
     *
     * <p>Writes an anonymised INFO audit log entry at the start of each invocation (FR-014).
     *
     * @param userEmail the authenticated user's email address (from JWT principal)
     * @return fully assembled export document, never null
     */
    ReefDataExportTo buildExportForUser(String userEmail);
}
