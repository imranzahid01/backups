package com.imranzahid.backup.util;

import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author imranzahid Date: 12/25/14 Time: 10:00 AM
 */
public class EmailUtility {
  private static Logger log = Logger.getLogger(EmailUtility.class);
  private static final Properties properties = System.getProperties();
  static {
    properties.setProperty("mail.debug", "false");
    properties.setProperty("mail.transport.protocol", "smtp");
    properties.setProperty("mail.smtp.host", "smtp.mandrillapp.com");
    properties.setProperty("mail.smtp.port", "587");
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.user", "emails@dsibtm.com");
    properties.setProperty("mail.smtp.password", "");
  }
  private static final Map<String, String> headers = new HashMap<>();
  static {
    headers.put("X-MC-Tags", "backup");
  }

  private EmailUtility() {}

  public static boolean sendEmail(@Nonnull String to, @Nonnull String subject, @Nonnull String email) {
    return sendEmail(to, subject, email, email);
  }

  public static boolean sendEmail(@Nonnull String to, @Nonnull String subject, @Nonnull String email,
                                  @Nonnull String content) {
    return sendEmail(new String[]{to}, subject, email, content);
  }

  public static boolean sendEmail(@Nonnull String[] to, @Nonnull String subject, @Nonnull String email,
                                  @Nonnull String content) {
    Session session = Session.getDefaultInstance(properties);

    MimeMessage message = new MimeMessage(session);
    try {
      for (String key : headers.keySet()) {
        String value = headers.get(key);
        message.addHeader(key, value);
      }
      message.setFrom(new InternetAddress("emails@dsibtm.com", "BTM Research Emails"));
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to[0]));
      if (to.length > 1) {
        for (int i = 1; i < to.length; i++) {
          message.addRecipient(Message.RecipientType.CC, new InternetAddress(to[i]));
        }
      }
      message.setSubject(subject);
      message.setText(email);
      message.setContent(content, "text/html");
      //Transport.send(message, properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
      return true;
    } catch (MessagingException | UnsupportedEncodingException e) {
      log.error("Unable to send email", e);
    }
    return false;
  }
}
