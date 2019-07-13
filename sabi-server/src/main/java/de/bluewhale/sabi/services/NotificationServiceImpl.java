/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @author Stefan Schubert
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    JavaMailSender mailer;

    @Value("${sabi.mailvalidation.url}")
    String mailValidationURL;

    @Override
    public void sendValidationMail(UserTo createdUser) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(createdUser.getEmail());
        helper.setSubject("sabi Account Validation");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine (userTO) extract sabi target URL from application properties
        helper.setText("<html><body>" +
                "<h1>Welcome to sabi</h1>" +
                "<p>To activate your account and make use of sabi we require to verify your email-address." +
                "To do so, please click on the following link or copy paste it into your browser:</p>" +
                mailValidationURL + "email/" + createdUser.getEmail() + "/validation/" + createdUser.getValidationToken() + "<br/ >" +
                "</body></html>", true);

        mailer.send(message);
    }

    @Override
    public void sendWelcomeMail(String email) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject("Sabi account activated");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine ggf. DISCLAIMER/ Nutzungsbedingungen
        helper.setText("<html><body>" +
                "<h1>Successful registration</h1>" +
                "<p>Your account has been activated." +
                "you can now login into sabi with your credentials.</p>" +
                "</body></html>", true);

        mailer.send(message);

    }


    @Override
    public void sendPasswordResetToken(String email, String resetValidationToken) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject("Your Sabi password reset request");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine
        helper.setText("<html><body>" +
                "<h1>Password reset token</h1>" +
                "<p>You (or someone) has requested to reset your password for your sabi account." +
                "Please use the following token within the password reset form: <b>" + resetValidationToken + "</b>" +
                "</p> " +
                "<p></p>" +
                "</body></html>", true);
        mailer.send(message);
    }

    @Override
    public void sendPasswordResetConfirmation(String emailAddress) throws MessagingException {

        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(emailAddress);
        helper.setSubject("Your Sabi password reset request");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine
        helper.setText("<html><body>" +
                "<h1>Password reset confirmed</h1>" +
                "<p>As requested your password has been changed. Please re-login or reconfigure your app to use" +
                "the new one from now on.</b>" +
                "</p> " +
                "<p></p>" +
                "</body></html>", true);
        mailer.send(message);
    }
}
