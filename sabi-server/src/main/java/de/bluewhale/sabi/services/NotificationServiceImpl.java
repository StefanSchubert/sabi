/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Stefan Schubert
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    JavaMailSender mailer;

    @Value("${sabi.mailvalidation.url}")
    String mailValidationURL;

    @Value("${spring.mail.username}")
    String senderAddress;

    @Value("${sabi.admin.users:admin@sabi-project.net}")
    String adminUsers;

    @Override
    public void sendValidationMail(UserTo createdUser) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        Locale usersLocale = (createdUser.getLanguage()==null?Locale.ENGLISH: new Locale(createdUser.getLanguage()));
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/RegistrationMessages", usersLocale);
        String headline = bundle.getString("email.verify.token.request.headline");
        String text = bundle.getString("email.verify.token.request.txt");

        String verifyLink = mailValidationURL + "email/" + createdUser.getEmail() + "/validation/" + createdUser.getValidationToken();
        verifyLink = verifyLink.trim();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(createdUser.getEmail());
        helper.setSubject("sabi Account Validation");
        helper.setFrom(senderAddress);

        helper.setText(String.format(usersLocale, "<html><body>" +
                "<h1>%s</h1>" +
                "<p>%s</p>" +
                "<a href=\"%s\">%s</a> <br/ >" +
                "</body></html>",headline,text,verifyLink,verifyLink), true);

        mailer.send(message);
    }

    @Override
    public void sendWelcomeMail(String email) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject("Sabi account activated");
        helper.setFrom(senderAddress);

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
        helper.setFrom(senderAddress);

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
        helper.setFrom(senderAddress);

        // todo i18n Textbausteine
        helper.setText("<html><body>" +
                "<h1>Password reset confirmed</h1>" +
                "<p>As requested your password has been changed. Please re-login or reconfigure your app to use" +
                " the new one from now on.</b>" +
                "</p> " +
                "<p></p>" +
                "</body></html>", true);
        mailer.send(message);
    }

    @Override
    public void sendCatalogueProposalNotification(String catalogueType, String scientificName) throws MessagingException {
        if (adminUsers == null || adminUsers.isBlank()) return;
        for (String adminEmail : adminUsers.split(",")) {
            adminEmail = adminEmail.trim();
            if (adminEmail.isEmpty()) continue;
            MimeMessage message = mailer.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(adminEmail);
            helper.setSubject("[sabi] New " + catalogueType + " Catalogue Proposal");
            helper.setFrom(senderAddress);
            helper.setText("<html><body>" +
                    "<h2>New Catalogue Proposal</h2>" +
                    "<p>A user has submitted a new <b>" + catalogueType + "</b> catalogue proposal:</p>" +
                    "<p><b>Scientific name:</b> " + scientificName + "</p>" +
                    "<p>Please review the proposal in the <a href=\"https://sabi-project.net/secured/admin/catalogueDashboard.xhtml\">Admin Catalogue Dashboard</a>.</p>" +
                    "</body></html>", true);
            mailer.send(message);
        }
    }
}
