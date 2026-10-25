/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    JavaMailSender mailer;

    @Value("${sabi.mailvalidation.url}")
    String mailValidationURL;

    @Value("${spring.mail.username}")
    String senderAddress;

    /** No-reply address for reminder emails (R4: NOT spring.mail.username). */
    @Value("${sabi.email.no-reply-address}")
    String noReplyAddress;

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

    @Override
    public void sendMeasurementReminderEmail(MeasurementReminderEmail payload, Locale locale) throws MessagingException {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/ReminderEmailMessages", locale);

        String subject = bundle.getString("reminder.email.subject");
        String greeting = MessageFormat.format(bundle.getString("reminder.email.greeting"), payload.username());
        String intro = bundle.getString("reminder.email.intro");
        String unitHeader = bundle.getString("reminder.email.unit.header");
        String valueHeader = bundle.getString("reminder.email.value.header");
        String dateHeader = bundle.getString("reminder.email.date.header");
        String noMeasurement = bundle.getString("reminder.email.no_measurement");
        String measurementLinkLabel = bundle.getString("reminder.email.measurement_link_label");
        String profileLinkLabel = bundle.getString("reminder.email.profile_link_label");
        String footer = bundle.getString("reminder.email.footer");

        MimeMessage message = mailer.createMimeMessage();
        // Standard multipart/alternative (plain text + HTML, contracts/email-template.md FR-006).
        // Built directly with jakarta.mail: since Spring 7, MimeMessageHelper(message, true) no
        // longer produces a top-level multipart/alternative — it nests the alternative under
        // multipart/mixed + multipart/related, which breaks the contract and simple mail clients.
        message.setFrom(new InternetAddress(noReplyAddress));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(payload.emailAddress()));
        message.setSubject(subject, "UTF-8");

        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(buildPlainTextBody(greeting, intro, unitHeader, valueHeader, dateHeader,
                noMeasurement, measurementLinkLabel, profileLinkLabel, footer, payload, locale), "UTF-8");
        alternative.addBodyPart(textPart);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildHtmlBody(greeting, intro, unitHeader, valueHeader, dateHeader,
                noMeasurement, measurementLinkLabel, profileLinkLabel, footer, payload, locale),
                "text/html; charset=UTF-8");
        alternative.addBodyPart(htmlPart);

        message.setContent(alternative);

        // Refresh the in-memory Content-Type headers of the body parts. jakarta.mail only
        // updates them during saveChanges()/serialization; without this, a freshly built
        // MimeBodyPart still reports the default "text/plain" content type in memory, which
        // breaks MIME inspection of the message object (e.g. with a mocked JavaMailSender).
        // Harmless in production: Spring's Transport calls saveChanges() before writing.
        message.saveChanges();

        mailer.send(message);
        // PII rule: log user_id= only, never the email address
        log.info("Sent measurement reminder email for user_id={}", payload.userId());
    }

    private String buildPlainTextBody(String greeting, String intro, String unitHeader, String valueHeader,
                                      String dateHeader, String noMeasurement, String measurementLinkLabel,
                                      String profileLinkLabel, String footer, MeasurementReminderEmail payload,
                                      Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(greeting).append("\n\n").append(intro).append("\n\n");
        sb.append(unitHeader).append(" | ").append(valueHeader).append(" | ").append(dateHeader).append("\n");
        for (MeasurementReminderEmail.OverdueUnit unit : payload.overdueUnits()) {
            sb.append(unit.unitSign()).append(" | ")
                    .append(formatValue(unit.lastValue(), locale, noMeasurement)).append(" | ")
                    .append(formatDate(unit.lastMeasuredOn(), locale, noMeasurement))
                    .append("\n");
        }
        sb.append("\n").append(measurementLinkLabel).append(": ").append(payload.measurementPageUrl()).append("\n");
        sb.append(profileLinkLabel).append(": ").append(payload.profilePageUrl()).append("\n\n");
        sb.append(footer).append("\n");
        return sb.toString();
    }

    private String buildHtmlBody(String greeting, String intro, String unitHeader, String valueHeader,
                                 String dateHeader, String noMeasurement, String measurementLinkLabel,
                                 String profileLinkLabel, String footer, MeasurementReminderEmail payload,
                                 Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>").append(escapeHtml(greeting)).append("</p>");
        sb.append("<p>").append(escapeHtml(intro)).append("</p>");
        sb.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">");
        sb.append("<tr><th>").append(escapeHtml(unitHeader)).append("</th>")
                .append("<th>").append(escapeHtml(valueHeader)).append("</th>")
                .append("<th>").append(escapeHtml(dateHeader)).append("</th></tr>");
        for (MeasurementReminderEmail.OverdueUnit unit : payload.overdueUnits()) {
            sb.append("<tr><td>").append(escapeHtml(unit.unitSign())).append("</td>")
                    .append("<td>").append(escapeHtml(formatValue(unit.lastValue(), locale, noMeasurement))).append("</td>")
                    .append("<td>").append(escapeHtml(formatDate(unit.lastMeasuredOn(), locale, noMeasurement))).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p><a href=\"").append(escapeHtml(payload.measurementPageUrl())).append("\">")
                .append(escapeHtml(measurementLinkLabel)).append("</a></p>");
        sb.append("<p><a href=\"").append(escapeHtml(payload.profilePageUrl())).append("\">")
                .append(escapeHtml(profileLinkLabel)).append("</a></p>");
        sb.append("<p>").append(escapeHtml(footer)).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String formatValue(Float value, Locale locale, String noMeasurementPlaceholder) {
        if (value == null) {
            return noMeasurementPlaceholder;
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(value);
    }

    private String formatDate(LocalDateTime dateTime, Locale locale, String noMeasurementPlaceholder) {
        if (dateTime == null) {
            return noMeasurementPlaceholder;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        return dateTime.format(formatter);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
