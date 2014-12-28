package com.imranzahid.backup.jobs;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.imranzahid.backup.entity.Databases;
import com.imranzahid.backup.entity.JobsData;
import com.imranzahid.backup.util.EmailUtility;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
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
@DisallowConcurrentExecution
public class BackupDatabaseJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(BackupDatabaseJob.class);
  private static final DateFormat SQL_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
  private static final String TAG = "BackupDatabaseJob";

  @Override public void execute(JobExecutionContext context) throws JobExecutionException {
    Calendar currentTime = Calendar.getInstance();
    String guid = context.getMergedJobDataMap().getString("guid");
    String lastRunOn = SQL_FORMAT.format(currentTime.getTime());
    log.info(TAG + " is executing for " + guid + " at " + lastRunOn);
    Databases databases = (Databases) context.getMergedJobDataMap().get("jobsData");
    sendEmail(databases, TAG + " Start", "Database Backup Job is executing at: " + lastRunOn);
    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      StringBuilder messages = new StringBuilder();
      for (Databases.Database database : databases.getDatabases()) {
        messages.append(backup(databases, database)).append("\n");
      }
      sendEmail(databases, TAG + " End",
                "Database Backup Job completed in: " + stopwatch.stop().elapsed(TimeUnit.MINUTES) +
                " with message: " + messages.toString());
    }
    catch (Exception e) {
      Throwable rootCause = Throwables.getRootCause(e);
      log.error("Unable to execute Database Backup Job: " + guid, rootCause);
      JobExecutionException e2 = new JobExecutionException(rootCause);
      e2.setUnscheduleAllTriggers(true);
      sendEmail(databases, TAG + " Error", "Unable to execute Database Backup Job: " + rootCause.getMessage());
      throw e2;
    }
  }

  @Nonnull private String backup(@Nonnull Databases databases, @Nonnull final Databases.Database database) throws Exception {
    Connection con = getConnection(databases.getServer(), database);
    String sqlTemplate = "BACKUP DATABASE [%s] TO DISK = N'%s' WITH NOFORMAT, NOINIT, NAME = N'%s-Full Database " +
        "Backup', SKIP, NOREWIND, NOUNLOAD,  STATS = 10";

    String base = databases.getBase();
    if (!base.endsWith(File.separator)) {
      base += File.separator;
    }
    String fileName;
    Databases.FileFormat fileFormat = databases.getFileFormat();
    if (fileFormat != null) {
      String fileFormatTemplate = fileFormat.getTemplate();
      List<Databases.FileFormatParam> params = fileFormat.getParams();
      Collections.sort(params, new Comparator<Databases.FileFormatParam>() {
        @Override public int compare(Databases.FileFormatParam left, Databases.FileFormatParam right) {
          return Integer.compare(left.getOrdinal(), right.getOrdinal());
        }
      });
      Iterable<String> transform = Iterables.transform(params, new Function<Databases.FileFormatParam, String>() {
        @Nullable @Override public String apply(Databases.FileFormatParam input) {
          switch (input.getParam().toUpperCase()) {
            case "DB_NAME":
              return database.getName();
            case "TIMESTAMP":
              return new SimpleDateFormat(input.getPattern()).format(new Date());
          }
          return null;
        }
      });
      fileName = String.format(fileFormatTemplate, Iterables.toArray(transform, Object.class));
    }
    else {
      fileName = database.getName() + ".bak";
    }

    StringBuilder location = new StringBuilder();
    if (!Strings.isNullOrEmpty(database.getLocation())) {
      location.append(database.getLocation());
      if (!database.getLocation().endsWith(File.separator)) {
        location.append(File.separatorChar);
      }
    }
    List<String> groupings;
    if (database.groupingsUsed()) {
      groupings = database.getGroupings();
    }
    else if (databases.groupingsUsed()) {
      groupings = databases.getGroupings();
    }
    else {
      groupings = Collections.emptyList();
    }
    for (String grouping : groupings) {
      switch (grouping.toUpperCase()) {
        case "DB_NAME":
          location.append(database.getName());
          break;
      }
      location.append(File.separatorChar);
    }

    String backupFile = base + location + fileName;
    Files.createParentDirs(new File(backupFile));
    String sql = String.format(sqlTemplate, database.getName(), backupFile, database.getName());
    log.info(sql);
    Statement st = con.createStatement();
    boolean execute = st.execute(sql);
    log.info("Executed: " + execute);
    closeConnections(st, con);
    StringBuilder message = new StringBuilder();
    message.append(String.format("Database %s is backed up to %s, fileSize = %s", database.getName(), backupFile,
                                 humanReadableByteCount(/*new File(backupFile).length()*/148480)));
    if (!Strings.isNullOrEmpty(database.getCompression())) {
      switch (database.getCompression().toLowerCase()) {
        case "zip":
          message.append(zipFile(backupFile));
          break;
      }
    }
    message.append(removeOldFiles(databases.getKeepFor(), base + location));
    return message.toString();
  }

  private String removeOldFiles(final long keep, String location) {
    if (keep <= 0) {
      return "";
    }
    final long currentTime = System.currentTimeMillis();
    File dir = new File(location);
    File[] files = dir.listFiles(new FileFilter() {
      @Override public boolean accept(File pathname) {
        return pathname.isFile() && pathname.canRead() && (currentTime - pathname.lastModified()) >= keep;
      }
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

  private String zipFile(String backupFile) throws ZipException {
    String zipFileName = Files.getNameWithoutExtension(backupFile) + ".zip";
    File file = new File(zipFileName);
    ZipFile zipFile = new ZipFile(file);
    zipFile.addFile(new File(backupFile), new ZipParameters());
    return String.format(", compressed to %s (%s)", zipFileName, humanReadableByteCount(file.length()));
  }

  private Connection getConnection(Databases.Server server, Databases.Database database) throws Exception {
    Class.forName("net.sourceforge.jtds.jdbc.Driver");
    String dbUrl = String.format("jdbc:jtds:sqlserver://%s:%d/%s;", server.getHost(), server.getPort(),
                                 database.getName());
    if (!Strings.isNullOrEmpty(server.getInstance())) {
      dbUrl += "instance=" + server.getInstance() + ";";
    }
    return DriverManager.getConnection(dbUrl, server.getUser(), server.getPass());
  }

  private void closeConnections(Statement st, Connection con) {
    try { if (st  != null) { st.close();  } } catch (SQLException ignored) { }
    try { if (con != null) { con.close(); } } catch (SQLException ignored) { }
  }

  private void sendEmail(JobsData data, String subject, String email) {
    EmailUtility.sendEmail(Iterables.toArray(data.getEmails(), String.class), subject, email, email);
  }

  public static String humanReadableByteCount(long bytes) {
    int unit = 1024;
    if (bytes < unit) return bytes + "B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = "KMGTPE".charAt(exp-1);
    return String.format("%.0f%sB", bytes / Math.pow(unit, exp), pre);
  }
}
