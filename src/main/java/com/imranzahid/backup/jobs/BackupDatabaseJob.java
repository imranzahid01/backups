package com.imranzahid.backup.jobs;

import com.imranzahid.backup.entity.*;
import com.imranzahid.backup.util.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author imranzahid Date: 12/25/14 Time: 4:40 PM
 */
public class BackupDatabaseJob {
  private static final Logger log = LoggerFactory.getLogger(BackupDatabaseJob.class);
  private static final DateFormat SQL_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
  private static final String TAG = "BackupDatabaseJob";
  private static final String SQL_TEMPLATE =
    "BACKUP DATABASE [%s] TO DISK = N'%s' WITH NOFORMAT, NOINIT, " +
    "NAME = N'%s-Full Database Backup', SKIP, NOREWIND, NOUNLOAD, COMPRESSION";
  private static final String FILE_SEPERATOR = File.separator;

  public void execute(Databases databases) {
    String executingOn = SQL_FORMAT.format(Calendar.getInstance().getTime());
    log.info(TAG + " is executing for at " + executingOn);
    EmailUtility.newEmail()
      .to(databases.getEmails())
      .withSubject("Database backups on " + databases.getName())
      .withHtmlEmail(Strings.replace(EmailUtility.txtDatabaseBackupStartTemplate,
          new String[]{"{{serverName}}", "{{startedOn}}"}, new String[]{databases.getName(), executingOn}),
        Strings.replace(EmailUtility.htmDatabaseBackupStartTemplate,
          new String[]{"{{serverName}}", "{{startedOn}}"}, new String[]{databases.getName(), executingOn}))
      .send();
    HealthCheckUtil healthCheckUtil = HealthCheckUtil.getInstance(databases.getHealthCheckUuid());
    healthCheckUtil.start();
    Stopwatch stopwatch = Stopwatch.createStarted();
    StringBuilder success = new StringBuilder();
    StringBuilder fail = new StringBuilder();
    for (Database database : databases.getDatabases()) {
      Response backup = backup(databases, database);
      if (backup.fail) {
        fail.append(backup.message).append("\n");
      }
      else {
        success.append(backup.message).append("\n");
      }
    }
    String message = "Database Backup Job completed in: " +
      stopwatch.stop().elapsed(TimeUnit.MINUTES) + " mins with message:\n";
    final String finalMessage;
    if (fail.length() > 0) {
      finalMessage = message + fail + "\n\n" + success;
      healthCheckUtil.fail(finalMessage);
    }
    else {
      finalMessage = message + success;
      healthCheckUtil.success(finalMessage);
    }
    try {
      healthCheckUtil.close();
    } catch (IOException ignored) { }
    EmailUtility.newEmail()
      .to(databases.getEmails())
      .withSubject("Database backups on " + databases.getName())
      .withHtmlEmail(Strings.replace(EmailUtility.txtDatabaseBackupEndTemplate,
          new String[]{"{{serverName}}", "{{message}}"}, new String[]{databases.getName(), finalMessage}),
        Strings.replace(EmailUtility.htmDatabaseBackupEndTemplate,
          new String[]{"{{serverName}}", "{{message}}"}, new String[]{databases.getName(), finalMessage}))
      .send();
  }

  @Nonnull private Response backup(@Nonnull Databases databases, @Nonnull final Database database) {
    final Response response = new Response();
    try {
      response.fail = false;
      response.message = doBackup(databases, database);
    }
    catch (Exception ex) {
      response.fail = true;
      response.message = ex.toString();
    }
    return response;
  }

  @Nonnull private String doBackup(@Nonnull Databases databases, @Nonnull final Database database) throws Exception {
    String base = databases.getBase();
    if (!base.endsWith(FILE_SEPERATOR)) {
      base += FILE_SEPERATOR;
    }
    String fileName;
    FileFormat fileFormat = databases.getFileFormat();
    if (fileFormat != null) {
      String fileFormatTemplate = fileFormat.getTemplate();
      List<FileFormatParam> params = fileFormat.getParams();
      params.sort(Comparator.comparingInt(FileFormatParam::getOrdinal));
      Object[] transform = params.stream().map(input -> {
        switch (input.getParam().toUpperCase()) {
          case "DB_NAME":
            return database.getName();
          case "TIMESTAMP":
            return new SimpleDateFormat(input.getPattern()).format(new Date());
        }
        return null;
      }).filter(Objects::nonNull).toArray(String[]::new);
      fileName = String.format(fileFormatTemplate, transform);
    }
    else {
      fileName = database.getName() + ".bak";
    }

    StringBuilder location = new StringBuilder();
    if (!Strings.isNullOrEmpty(database.getLocation())) {
      location.append(database.getLocation());
      if (!database.getLocation().endsWith(FILE_SEPERATOR)) {
        location.append(FILE_SEPERATOR);
      }
      location.append(database.getName()).append(FILE_SEPERATOR);
    }

    String backupFile = base + location + fileName;
    Files.createParentDirs(new File(backupFile));
    String sql = String.format(SQL_TEMPLATE, database.getName(), backupFile, database.getName());
    log.info(sql);
    Connection con = getConnection(databases.getDatabaseServer(), database);
    Statement st = con.createStatement();
    st.execute(sql);
    closeConnections(st, con);
    StringBuilder message = new StringBuilder();
    File backedupFile = new File(backupFile);
    String msg1 = String.format("Database %s is backed up to %s, fileSize = %s", database.getName(), backupFile,
                                humanReadableByteCount(backedupFile.length()));
    log.info(msg1);
    message.append(msg1);
    if ("zip".equalsIgnoreCase(database.getCompression())) {
      backedupFile = zipFile(backupFile);
      String msg2 = String.format(", compressed to %s (%s)", backedupFile.getName(),
                                  humanReadableByteCount(backedupFile.length()));
      log.info(msg2);
      message.append(msg2);
    }
    String msg3 = removeOldFiles(databases.getKeepFor(), base + location);
    if (!msg3.isBlank()) log.info(msg3);
    message.append(msg3);
    SftpServer sftpServer = databases.getSftpServer();
    if (database.isUpload() && sftpServer != null) {
      String msg4 = uploadFile(sftpServer, backedupFile, databases.getName(),
                               location.toString(), database.getName());
      log.info(msg4);
      message.append(". ").append(msg4);
    }
    return message.toString();
  }

  @Nonnull private String uploadFile(@Nonnull SftpServer sftpServer, @Nonnull File sourceFile,
                                     @Nonnull final String name, @Nullable String location,
                                     @Nonnull final String databaseName) {
    SftpClient sftp = new SftpClient(sftpServer);
    if (!sftp.connect()) {
      return "Unable to connect";
    }
    String path;
    FileFormat pathFormat = sftpServer.getPathFormat();
    if (pathFormat != null) {
      String template = pathFormat.getTemplate();
      List<FileFormatParam> params = pathFormat.getParams();
      params.sort(Comparator.comparingInt(FileFormatParam::getOrdinal));
      Object[] transform = params.stream().map(input -> {
        switch (input.getParam().toUpperCase()) {
          case "NAME":
            return name;
          case "LOCATION":
            return location != null ? location.replaceAll("\\\\", "/") : "";
          case "DB_NAME":
            return databaseName;
        }
        return null;
      }).filter(Objects::nonNull).toArray(String[]::new);
      path = String.format(template, transform).replaceAll("//", "/");
    }
    else {
      path = "";
    }
    if (!path.isBlank() && !sftp.mkdirs(path)) {
      return "Unable to create " + path + " directory";
    }
    if (!sftp.put(sourceFile.getName(), sourceFile)) {
      return "Unable to upload file";
    }
    String destFile = path + sourceFile.getName();
    if (!sourceFile.delete()) {
      log.error("Unable to delete source file " + sourceFile + " after uploading it to " + destFile);
    }
    sftp.close();
    return "Upload to SFTP as " + destFile;
  }

  @Nonnull private String removeOldFiles(final long keep, @Nonnull String location) {
    if (keep <= 0) {
      return "";
    }
    final long currentTime = System.currentTimeMillis();
    File dir = new File(location);
    File[] files = dir.listFiles(path -> {
      String pathName = path.getName().toLowerCase();
      boolean save = pathName.contains("save") || pathName.contains("safe") || pathName.contains("keep");
      return path.isFile() && path.canRead() && !save && (currentTime - path.lastModified()) >= keep;
    });
    if (files != null && files.length > 0) {
      StringBuilder message = new StringBuilder().append("\n\tCleaned up files: ");
      String sep = "";
      for (File file : files) {
        if (file.delete()) {
          message.append(sep).append(file.getName());
        }
        else {
          message.append(sep).append(file.getName()).append(" (can't delete)");
        }
        sep = ", ";
      }
      return message.toString();
    }
    return "";
  }

  @Nonnull private File zipFile(@Nonnull String backupFile) throws ZipException {
    File sourceFile = new File(backupFile);
    String zipFileName = getNameWithoutExtension(backupFile) + ".zip";
    File file = new File(zipFileName);
    ZipFile zipFile = new ZipFile(file);
    zipFile.addFile(sourceFile, new ZipParameters());
    if (!sourceFile.delete()) {
      log.error("Unable to delete source file " + backupFile + " after zipping it to " + zipFileName);
    }
    return file;
  }

  @Nonnull private String getNameWithoutExtension(@Nonnull String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  @Nonnull private Connection getConnection(@Nonnull DatabaseServer server,
                                            @Nonnull Database database) throws Exception {
    Class.forName("net.sourceforge.jtds.jdbc.Driver");
    String dbUrl = String.format("jdbc:jtds:sqlserver://%s:%d/%s;", server.getHost(), server.getPort(),
                                 database.getName());
    if (!Strings.isNullOrEmpty(server.getInstance())) {
      dbUrl += "instance=" + server.getInstance() + ";";
    }
    return DriverManager.getConnection(dbUrl, server.getUser(), server.getPass());
  }

  private void closeConnections(@Nullable Statement st, @Nullable Connection con) {
    try { if (st  != null) { st.close();  } } catch (SQLException ignored) { }
    try { if (con != null) { con.close(); } } catch (SQLException ignored) { }
  }

  private static String humanReadableByteCount(long bytes) {
    int unit = 1024;
    if (bytes < unit) return bytes + "B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = "KMGTPE".charAt(exp-1);
    return String.format("%.0f%sB", bytes / Math.pow(unit, exp), pre);
  }

  private static class Response {
    boolean fail;
    String message;
  }
}
