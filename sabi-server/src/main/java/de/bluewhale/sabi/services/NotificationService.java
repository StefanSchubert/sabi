/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import jakarta.mail.MessagingException;

import java.util.Locale;

/**
 * Provides services around user notifications.
 */
public interface NotificationService {

    void sendValidationMail(UserTo createdUser) throws MessagingException;

    void sendWelcomeMail(String email) throws MessagingException;

    void sendPasswordResetToken(String email, String resetValidationToken) throws MessagingException;

    void sendPasswordResetConfirmation(String emailAddress) throws MessagingException;

    /**
     * Notifies all admin users that a new catalogue proposal has been submitted.
     *
     * @param catalogueType  e.g. "Fish" or "Coral"
     * @param scientificName the scientific name of the proposed entry
     */
    void sendCatalogueProposalNotification(String catalogueType, String scientificName) throws MessagingException;

    /**
     * Sends the localized measurement reminder email (sabi-69) as multipart/alternative
     * (HTML + plain text) from the no-reply address to the user's verified email.
     *
     * @param payload the transient email payload built by {@link EmailReminderService}
     * @param locale  the user's locale for bundle selection and date/value formatting
     */
    void sendMeasurementReminderEmail(MeasurementReminderEmail payload, Locale locale) throws MessagingException;
}
