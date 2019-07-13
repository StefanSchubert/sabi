/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;

import javax.mail.MessagingException;

/**
 * Provides all required services for user notifications.
 */
public interface NotificationService {

    void sendValidationMail(UserTo createdUser) throws MessagingException;

    void sendWelcomeMail(String email) throws MessagingException;

    void sendPasswordResetToken(String email, String resetValidationToken) throws MessagingException;

    void sendPasswordResetConfirmation(String emailAddress) throws MessagingException;
}
