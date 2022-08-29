package com.imranzahid.backup.util;

import com.imranzahid.backup.entity.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BackupUtil {
  private static final Logger log = LoggerFactory.getLogger(BackupUtil.class);

  @Nullable public static Databases parseDatabaseXml() {
    Databases databases = null;
    try {
      String xmlFile = System.getProperty("backups");
      SAXBuilder saxBuilder = new SAXBuilder();
      Document document = saxBuilder.build(new File(xmlFile));
      Element rootElement = document.getRootElement();
      if (!rootElement.getName().equalsIgnoreCase("backups")) {
        throw new IOException("invalid xml file");
      }
      Element databasesElement = rootElement.getChild("databases");
      if (databasesElement != null) {
        databases = new Databases();
        parseDatabases(databases, databasesElement);
      }
    }
    catch (JDOMException | IOException e) {
      log.error("Unable to get scheduling information", e);
    }
    return databases;
  }

  private static void parseDatabases(Databases databases, Element databasesElement) {
    Element metaElement = databasesElement.getChild("meta");
    databases.setName(metaElement.getChildTextNormalize("name"));
    databases.setBase(metaElement.getChildTextNormalize("base"));

    Element fileformatElement = metaElement.getChild("fileformat");
    FileFormat fileFormat = databases.newFileFormat();
    fileFormat.setTemplate(fileformatElement.getChildTextNormalize("template"));
    /* params */ {
      Element paramsElement = fileformatElement.getChild("params");
      if (paramsElement != null) {
        List<Element> params = paramsElement.getChildren("param");
        if (params != null) {
          for (Element param : params) {
            fileFormat.addParam(param.getAttributeValue("ordinal", "0"), param.getAttributeValue("pattern", ""),
                                param.getTextNormalize());
          }
        }
      }
    }
    databases.setKeep(metaElement.getChildTextNormalize("keep"));
    Element emailsElement = metaElement.getChild("emails");
    if (emailsElement != null) {
      List<Element> emails = emailsElement.getChildren("email");
      if (emails != null) {
        for (Element email : emails) {
          databases.getEmails().add(email.getTextNormalize());
        }
      }
    }
    databases.setHealthCheckUuid(metaElement.getChildTextNormalize("healthcheck"));

    Element serverElement = metaElement.getChild("server");
    DatabaseServer databaseServer = databases.newDatabaseServer();
    databaseServer.setHost(serverElement.getChildTextNormalize("host"));
    databaseServer.setPort(serverElement.getChildTextNormalize("port"));
    databaseServer.setInstance(serverElement.getChildTextNormalize("instance"));
    databaseServer.setUser(serverElement.getChildTextNormalize("user"));
    databaseServer.setPass(serverElement.getChildTextNormalize("password"));

    Element sftpElement = metaElement.getChild("sftp");
    if (sftpElement != null) {
      SftpServer sftpServer = databases.newSftpServer();
      sftpServer.setEnabled(Boolean.parseBoolean(sftpElement.getAttributeValue("enabled", "true")));
      sftpServer.setHost(sftpElement.getChildTextNormalize("host"));
      sftpServer.setPort(sftpElement.getChildTextNormalize("port"));
      sftpServer.setUser(sftpElement.getChildTextNormalize("user"));
      sftpServer.setPass(sftpElement.getChildTextNormalize("pass"));
      String limit = sftpElement.getChildTextNormalize("limit");
      try {
        if (limit != null && !limit.isBlank()) {
          StringBuilder pre = new StringBuilder();
          char unit = 'B';
          for (int i = 0; i < limit.length(); i++) {
            char ch = limit.charAt(i);
            if (Character.isDigit(ch)) {
              pre.append(ch);
              continue;
            }
            if (Character.isAlphabetic(ch)) {
              unit = Character.toUpperCase(ch);
              break;
            }
          }
          long amount = Long.parseLong(pre.toString());
          switch (unit) {
            case 'b' : case 'B' : sftpServer.setLimit(amount); break;
            case 'k' : case 'K' : sftpServer.setLimit(amount * 1024); break;
            case 'm' : case 'M' : sftpServer.setLimit(amount * 1024 * 1024); break;
            case 'g' : case 'G' : sftpServer.setLimit(amount * 1024 * 1024 * 1024); break;
            case 't' : case 'T' : sftpServer.setLimit(amount * 1024 * 1024 * 1024 * 1024); break;
          }
        }
      } catch (Exception ex) {
        log.error("Unable to set the limit of " + limit, ex);
      }

      Element pathElement = sftpElement.getChild("path");
      FileFormat pathFormat = sftpServer.newPathFormat();
      pathFormat.setTemplate(pathElement.getChildTextNormalize("template"));
      /* params */ {
        Element paramsElement = pathElement.getChild("params");
        if (paramsElement != null) {
          List<Element> params = paramsElement.getChildren("param");
          if (params != null) {
            for (Element param : params) {
              pathFormat.addParam(param.getAttributeValue("ordinal", "0"), param.getAttributeValue("pattern", ""),
                                  param.getTextNormalize());
            }
          }
        }
      }
    }

    List<Element> databaseElements = databasesElement.getChildren("database");
    if (databaseElements != null) {
      for (Element databaseElement : databaseElements) {
        Database database = databases.newDatabase(databaseElement.getChildTextNormalize("name"));
        database.setLocation(databaseElement.getChildTextNormalize("location"));
        database.setCompression(databaseElement.getChildTextNormalize("compression"));
        String upload = databaseElement.getChildTextNormalize("upload");
        if ("false".equalsIgnoreCase(upload)) {
          database.setUpload(false);
        }
        String keep = databaseElement.getChildTextNormalize("keep");
        if (Strings.isNullOrEmpty(keep)) {
          keep = databases.getKeep();
        }
        database.setKeep(keep);
        databases.getDatabases().add(database);
      }
    }
  }

  public static long getKeepFor(@Nullable String keep) {
    if (Strings.isNullOrEmpty(keep)) {
      return -1;
    }
    StringBuilder number = new StringBuilder();
    StringBuilder unit = new StringBuilder();
    for (int i = 0; i < keep.length(); i++) {
      char ch = keep.charAt(i);
      if (Character.isDigit(ch)) {
        number.append(ch);
      }
      else if (Character.isLetter(ch)) {
        unit.append(ch);
      }
    }
    Duration duration;
    try {
      long num = Long.parseLong(number.toString());
      switch(unit.toString()) {
        case "S": case "s": duration = Duration.ofSeconds(num); break;
        case "m":           duration = Duration.ofMinutes(num); break;
        case "h": case "H": duration = Duration.ofHours(num); break;
        case "D": case "d": duration = Duration.ofDays(num); break;
        case "w": case "W": duration = Duration.ofSeconds(
          Math.multiplyExact(num, ChronoUnit.WEEKS.getDuration().getSeconds())); break;
        case "M":           duration = Duration.ofSeconds(
          Math.multiplyExact(num, ChronoUnit.MONTHS.getDuration().getSeconds())); break;
        case "y": case "Y": duration = Duration.ofSeconds(
          Math.multiplyExact(num, ChronoUnit.YEARS.getDuration().getSeconds())); break;
        default : duration = Duration.ZERO; break;
      }
    } catch (NumberFormatException ignored) {
      duration = Duration.ZERO;
    }
    return duration.toMillis();
  }
}
