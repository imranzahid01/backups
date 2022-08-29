package com.imranzahid.backup.entity;

import com.imranzahid.backup.util.BackupUtil;
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
    return BackupUtil.getKeepFor(getKeep());
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
