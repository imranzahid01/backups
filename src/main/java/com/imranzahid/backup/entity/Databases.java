package com.imranzahid.backup.entity;

import com.imranzahid.backup.util.Strings;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author imranzahid Date: 12/25/14 Time: 5:16 PM
 */
public class Databases {
  private String name;
  private String base;
  private FileFormat fileFormat;
  private String keep;
  private final List<String> emails = new ArrayList<>();
  private String healthCheckUuid;
  private DatabaseServer databaseServer;
  private final List<Database> databases = new ArrayList<>();
  private SftpServer sftpServer;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBase() {
    return base;
  }

  public void setBase(String base) {
    this.base = base;
  }

  public FileFormat getFileFormat() {
    return fileFormat;
  }

  public FileFormat newFileFormat() {
    this.fileFormat = new FileFormat();
    return this.fileFormat;
  }

  public String getKeep() {
    return keep;
  }

  public void setKeep(String keep) {
    this.keep = keep;
  }

  public long getKeepFor() {
    if (Strings.isNullOrEmpty(getKeep())) {
      return -1;
    }
    StringBuilder number = new StringBuilder();
    StringBuilder unit = new StringBuilder();
    for (int i = 0; i < getKeep().length(); i++) {
      char ch = getKeep().charAt(i);
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
        case "w": case "W": duration = ChronoUnit.WEEKS.getDuration(); break;
        case "M":           duration = ChronoUnit.MONTHS.getDuration(); break;
        case "y": case "Y": duration = ChronoUnit.YEARS.getDuration(); break;
        default : duration = Duration.ZERO; break;
      }
    } catch (NumberFormatException ignored) {
      duration = Duration.ZERO;
    }
    return duration.toMillis();
  }

  public List<String> getEmails() {
    return emails;
  }

  public String getHealthCheckUuid() {
    return healthCheckUuid;
  }

  public void setHealthCheckUuid(String healthCheckUuid) {
    this.healthCheckUuid = healthCheckUuid;
  }

  public DatabaseServer getDatabaseServer() {
    return databaseServer;
  }

  public DatabaseServer newDatabaseServer() {
    this.databaseServer = new DatabaseServer();
    return this.databaseServer;
  }

  public SftpServer getSftpServer() {
    return sftpServer;
  }

  public SftpServer newSftpServer() {
    this.sftpServer = new SftpServer();
    return this.sftpServer;
  }

  public List<Database> getDatabases() {
    return databases;
  }

  public Database newDatabase(String name) {
    Database database = new Database();
    database.setName(name);
    return database;
  }
}
