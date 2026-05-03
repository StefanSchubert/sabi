/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

-- V1_5_0_10: Add estimated net volume column to aquarium table.
-- The net volume (size_net) represents the effective water volume after subtracting
-- rocks, substrate, and decoration — relevant for AI-based dosage recommendations.
ALTER TABLE sabi.aquarium
    ADD COLUMN size_net INT NULL COMMENT 'Estimated net water volume in liters (after subtracting rocks/decoration)';

