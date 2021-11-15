package com.imranzahid.backup.util;

import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author imranzahid Date: 12/25/14 Time: 10:00 AM
 */
public class EmailUtility {
  private static final Logger log = Logger.getLogger(EmailUtility.class);
  private static final List<String> categories = new ArrayList<>();
  static {
    categories.add("backup");
  }

  public static final String txtDatabaseBackupStartTemplate =
    "Dear {{userName}}\n\n" +
    "Database backup on '{{serverName}}' started on {{startedOn}}.\n\n" +
    "Sincerely,\n\n" +
    "BIN3DS/ BINDS\n\n" +
    "Bio-Informatics for Diagnostics, Discovery and Development Solution";
  public static final String htmDatabaseBackupStartTemplate =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
    "  <meta http-equiv=\"Content-Style-Type\" content=\"text/css\">\n" +
    "  <style type=\"text/css\">\n" +
    "    p {margin: 0; font: 12px Helvetica}\n" +
    "    p.high {min-height: 14px}\n" +
    "    p.italic {font-style:italic;}\n" +
    "    p.strong {font-weight:bold;}\n" +
    "    img {border:0;}\n" +
    "  </style>\n" +
    "</head>\n" +
    "<body>\n" +
    "  <p>Dear {{userName}}</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p>Database backup on '{{serverName}}' started on {{startedOn}}.</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p>Sincerely,</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"italic\">BIN3DS/ BINDS</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"italic strong\">Bio-Informatics for Diagnostics, Discovery\n" +
    "  and Development Solution</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p><img src=\"http://www.daedalussoftware.com/binds/img/binds_logo.png\" width=\"316\" " +
        "height=\"105\" alt=\"BINDS logo\" /></p>\n" +
    "</body>\n" +
    "</html>\n";

  public static final String txtDatabaseBackupEndTemplate =
    "Dear {{userName}}\n\n" +
    "Database backup on '{{serverName}}' completed with the following message\n\n{{message}}\n\n" +
    "Sincerely,\n\n" +
    "BIN3DS/ BINDS\n\n" +
    "Bio-Informatics for Diagnostics, Discovery and Development Solution";
  public static final String htmDatabaseBackupEndTemplate =
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
    "  <meta http-equiv=\"Content-Style-Type\" content=\"text/css\">\n" +
    "  <style type=\"text/css\">\n" +
    "    p {margin: 0; font: 12px Helvetica}\n" +
    "    p.high {min-height: 14px}\n" +
    "    p.italic {font-style:italic;}\n" +
    "    p.strong {font-weight:bold;}\n" +
    "    img {border:0;}\n" +
    "  </style>\n" +
    "</head>\n" +
    "<body>\n" +
    "  <p>Dear {{userName}}</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p>Database backup on '{{serverName}}' completed with the following message:</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p>{{message}}</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p>Sincerely,</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"italic\">BIN3DS/ BINDS</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p class=\"italic strong\">Bio-Informatics for Diagnostics, Discovery\n" +
    "  and Development Solution</p>\n" +
    "  <p class=\"high\"><br></p>\n" +
    "  <p><img src=\"http://www.daedalussoftware.com/binds/img/binds_logo.png\" width=\"316\" " +
        "height=\"105\" alt=\"BINDS logo\" /></p>\n" +
    "</body>\n" +
    "</html>\n";

  private EmailUtility() {}

  private static boolean send(@Nonnull Iterator<String> recipients, @Nonnull String subject,
                              @Nullable String text, @Nullable String content) {
    if (!recipients.hasNext()) {
      return true;
    }
    Properties properties = getProperties();
    if (!Boolean.parseBoolean(properties.getProperty("mail.feature.enabled", "true"))) {
      log.error("Mail feature disabled. Original message:\n" + text);
      return false;
    }
    Session session = Session.getDefaultInstance(properties);

    MimeMessage message = new MimeMessage(session);
    try {
      if (categories.size() > 0) {
        StringBuilder jsonData = new StringBuilder().append("{\"category\": [");
        String sep = "";
        for (String category : categories) {
          jsonData.append(sep).append('"').append(category).append('"');
        }
        jsonData.append("]}");
        message.addHeader("X-SMTPAPI", jsonData.toString());
      }
      message.setFrom(new InternetAddress("emails@dsibtm.com", "BTM Backups"));
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipients.next()));
      while (recipients.hasNext()) {
        message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipients.next()));
      }
      message.setSubject(subject);
      message.setText(text);
      message.setContent(content, "text/html");
      Transport.send(message, properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
      return true;
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("Unable to send email due to: " + e + " Original message:\n" + text);
    }
    return false;
  }

  private static Properties getProperties() {
    String password = System.getProperty("mailSmtpPassword", "");
    Properties properties = System.getProperties();
    properties.setProperty("mail.debug", "false");
    properties.setProperty("mail.transport.protocol", "smtp");
    properties.setProperty("mail.feature.enabled", String.valueOf(!password.isBlank()));
    properties.setProperty("mail.smtp.host", "smtp.sendgrid.net");
    properties.setProperty("mail.smtp.port", "587");
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.user", "apikey");
    properties.setProperty("mail.smtp.password", password);
    return properties;
  }

  public static class EmailUtilityBuilder {
    private Iterator<String> recipients;
    private String subject;
    private String html;
    private String text;

    public EmailUtilityBuilder to(@Nonnull Iterable<String> recipients) {
      this.recipients = Objects.requireNonNull(recipients, "The recipients cannot be null").iterator();
      return this;
    }

    public EmailUtilityBuilder withSubject(@Nonnull String subject) {
      this.subject = Objects.requireNonNull(subject, "Subject cannot be null");
      return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public EmailUtilityBuilder withPlainEmail(@Nonnull String email) {
      return withHtmlEmail(email, email);
    }

    public EmailUtilityBuilder withHtmlEmail(@Nonnull String text, @Nonnull String html) {
      this.text = Objects.requireNonNull(text, "E-Mail Text content cannot be null");
      this.html = Objects.requireNonNull(html, "E-Mail HTML content cannot be null");
      return this;
    }

    public boolean send() {
      return EmailUtility.send(recipients, subject, text, html);
    }
  }

  public static EmailUtilityBuilder newEmail() {
    return new EmailUtilityBuilder();
  }
}
