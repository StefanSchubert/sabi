/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.BodyPart;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_12_2_2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;

/**
 * Email-content tests for the measurement reminder email (sabi-69, T014):
 * structure, localization for all 6 bundles (de/en/es/fr/it + fallback),
 * locale-specific value/date formatting, deep links and delivery parameters
 * (contracts/email-template.md).
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NotificationServiceReminderEmailTest {

    private static final String NO_REPLY_ADDRESS = "no-reply@sabi-project.net";
    private static final String USER_EMAIL = "reminder-test@example.com";
    private static final String USERNAME = "TestUser";
    private static final LocalDateTime LAST_MEASURED_ON = LocalDateTime.of(2026, 10, 20, 9, 30);
    private static final float LAST_VALUE = 5.5f;

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_12_2_2);

    @Autowired
    NotificationServiceImpl notificationService;

    @MockitoBean
    JavaMailSender mailer;

    private List<MimeMessage> sentMessages;

    @BeforeEach
    void setUpMailer() throws Exception {
        sentMessages = new ArrayList<>();
        given(mailer.createMimeMessage()).willReturn(new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0, MimeMessage.class));
            return null;
        }).when(mailer).send(ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void testReminderEmailContent_forAllLocales() throws Exception {
        // de/en/es/fr/it + fallback (pt has no dedicated bundle → default English bundle)
        Locale[] locales = {
                Locale.GERMAN, Locale.ENGLISH, new Locale("es"),
                new Locale("fr"), new Locale("it"), new Locale("pt")
        };

        for (Locale locale : locales) {
            MeasurementReminderEmail payload = buildPayload();

            notificationService.sendMeasurementReminderEmail(payload, locale);

            MimeMessage sent = sentMessages.get(sentMessages.size() - 1);
            ResourceBundle bundle = ResourceBundle.getBundle("i18n/ReminderEmailMessages", locale);

            // delivery parameters (FR-006)
            assertEquals(bundle.getString("reminder.email.subject"), subjectOf(sent), "subject for " + locale);
            assertEquals(NO_REPLY_ADDRESS, sent.getFrom()[0].toString(), "from for " + locale);
            assertEquals(USER_EMAIL, sent.getAllRecipients()[0].toString(), "to for " + locale);

            // multipart/alternative with text/plain + text/html
            MimeMultipart multipart = multipartOf(sent);
            String html = partContent(multipart, "text/html");
            String text = partContent(multipart, "text/plain");
            assertNotNull(html, "html part for " + locale);
            assertNotNull(text, "text part for " + locale);

            String expectedValue = formatValue(locale);
            String expectedDate = formatDate(locale);
            String noMeasurement = bundle.getString("reminder.email.no_measurement");

            for (String part : List.of(html, text)) {
                assertTrue(part.contains(USERNAME), "greeting must address the user (" + locale + ")");
                assertTrue(part.contains("NO3 (mg/l)"), "unit sign must be present (" + locale + ")");
                assertTrue(part.contains(expectedValue), "value must be locale-formatted (" + locale + ")");
                assertTrue(part.contains(expectedDate), "date must be locale-formatted (" + locale + ")");
                assertTrue(part.contains(noMeasurement), "placeholder for never-measured unit (" + locale + ")");
                assertTrue(part.contains("https://sabi-project.net/secured/measureView.xhtml"),
                        "measurement deep link (" + locale + ")");
                assertTrue(part.contains("https://sabi-project.net/secured/userProfile.xhtml"),
                        "profile deep link (" + locale + ")");
            }

            // table headers and footer from the bundle
            for (String key : List.of("reminder.email.unit.header", "reminder.email.value.header",
                    "reminder.email.date.header", "reminder.email.footer")) {
                assertTrue(html.contains(bundle.getString(key)), key + " in html (" + locale + ")");
                assertTrue(text.contains(bundle.getString(key)), key + " in text (" + locale + ")");
            }
        }
    }

    @Test
    void testReminderEmailSubject_differsPerLocale() throws Exception {
        String german = ResourceBundle.getBundle("i18n/ReminderEmailMessages", Locale.GERMAN)
                .getString("reminder.email.subject");
        String english = ResourceBundle.getBundle("i18n/ReminderEmailMessages", Locale.ENGLISH)
                .getString("reminder.email.subject");
        assertNotEquals(german, english, "de and en subjects must be translated");

        notificationService.sendMeasurementReminderEmail(buildPayload(), Locale.GERMAN);
        assertEquals(german, subjectOf(sentMessages.get(sentMessages.size() - 1)));

        notificationService.sendMeasurementReminderEmail(buildPayload(), Locale.ENGLISH);
        assertEquals(english, subjectOf(sentMessages.get(sentMessages.size() - 1)));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private MeasurementReminderEmail buildPayload() {
        return new MeasurementReminderEmail(
                42L,
                USER_EMAIL,
                USERNAME,
                "en",
                List.of(
                        new MeasurementReminderEmail.OverdueUnit("NO3 (mg/l)", LAST_VALUE, LAST_MEASURED_ON),
                        new MeasurementReminderEmail.OverdueUnit("PO4 (mg/l)", null, null)),
                "https://sabi-project.net/secured/measureView.xhtml",
                "https://sabi-project.net/secured/userProfile.xhtml");
    }

    private static String formatValue(Locale locale) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(LAST_VALUE);
    }

    private static String formatDate(Locale locale) {
        return LAST_MEASURED_ON.format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale));
    }

    private static String subjectOf(MimeMessage message) throws Exception {
        return message.getSubject();
    }

    private static MimeMultipart multipartOf(MimeMessage message) throws Exception {
        Object content = message.getContent();
        assertTrue(content instanceof MimeMultipart, "reminder email must be multipart/alternative");
        return (MimeMultipart) content;
    }

    private static String partContent(MimeMultipart multipart, String mimeType) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType(mimeType)) {
                return (String) part.getContent();
            }
        }
        return null;
    }

}
