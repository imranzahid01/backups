package com.imranzahid.backup.entity;

import com.google.common.base.Strings;

import java.util.*;

/**
 * @author imranzahid Date: 12/25/14 Time: 5:16 PM
 */
public class Databases implements JobsData, Groupings {
  private static final int SECOND = 60;
  private static final int MINUTE = 60 * SECOND;
  private static final int HOUR = 60 * MINUTE;
  private static final int DAY = 24 * HOUR;
  private static final int WEEK = 7 * DAY;
  private static final int MONTH = 30 * DAY;
  private static final int YEAR = 54 * WEEK;

  private String name;
  private String base;
  private String cron;
  private FileFormat fileFormat;
  private final List<String> groupings = new ArrayList<>();
  private String keep;
  private final List<String> emails = new ArrayList<>();
  private String healthCheckUuid;
  private Server server;
  private final List<Database> databases = new ArrayList<>();

  @Override public String getName() {
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

  @Override public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public FileFormat getFileFormat() {
    return fileFormat;
  }

  public FileFormat newFileFormat() {
    this.fileFormat = new FileFormat();
    return this.fileFormat;
  }

  @Override public List<String> getGroupings() {
    Optional<String> none = groupings.stream().filter(g -> g != null && g.equalsIgnoreCase("NONE")).findFirst();
    if (none.isPresent()) {
      return Collections.emptyList();
    }
    return groupings;
  }

  public boolean groupingsUsed() {
    return groupings.size() > 0;
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
    try {
      long num = Long.parseLong(number.toString());
      switch(unit.toString()) {
        case "S": case "s": return num * SECOND;
        case "m":           return num * MINUTE;
        case "h": case "H": return num * HOUR;
        case "D": case "d": return num * DAY;
        case "w": case "W": return num * WEEK;
        case "M":           return num * MONTH;
        case "y": case "Y": return num * YEAR;
      }
    } catch (NumberFormatException ignored) { }
    return -1;
  }

  @Override public List<String> getEmails() {
    return emails;
  }

  @Override public String getHealthCheckUuid() {
    return healthCheckUuid;
  }

  public void setHealthCheckUuid(String healthCheckUuid) {
    this.healthCheckUuid = healthCheckUuid;
  }

  public Server getServer() {
    return server;
  }

  public Server newServer() {
    this.server = new Server();
    return this.server;
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
