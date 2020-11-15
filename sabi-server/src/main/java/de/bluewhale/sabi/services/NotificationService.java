/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;

import javax.mail.MessagingException;

/**
 * Provides services around user notifications.
 */
public interface NotificationService {

    void sendValidationMail(UserTo createdUser) throws MessagingException;

    void sendWelcomeMail(String email) throws MessagingException;

    void sendPasswordResetToken(String email, String resetValidationToken) throws MessagingException;

    void sendPasswordResetConfirmation(String emailAddress) throws MessagingException;
}
