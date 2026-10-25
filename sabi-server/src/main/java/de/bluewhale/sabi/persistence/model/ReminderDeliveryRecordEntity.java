/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Delivery record for one email reminder cycle of one user.
 * The unique (user_id, cycle_date) pair is the idempotency key: duplicate
 * scheduler runs find the existing record and skip. Status is SENT or FAILED;
 * attempts counts failed send attempts within the cycle (max 3).
 * GDPR: records are removed via CASCADE when the user is deleted.
 *
 * @author Stefan Schubert
 */
@Table(name = "reminder_delivery_record", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"user"}, callSuper = false)
public class ReminderDeliveryRecordEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    private Long id;

    /**
     * Owner-side of the relationship: FK → users.id (ON DELETE CASCADE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * The due date the cycle refers to (= the user's next send date at evaluation).
     */
    @Basic
    @Column(name = "cycle_date", nullable = false)
    private LocalDate cycleDate;

    /**
     * Delivery status: SENT or FAILED.
     */
    @Basic
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    /**
     * Number of failed send attempts within this cycle (0-3).
     */
    @Basic
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /**
     * Timestamp when the email was actually dispatched; NULL until success.
     */
    @Column(name = "sent_at", nullable = true)
    private LocalDateTime sentAt;

}
