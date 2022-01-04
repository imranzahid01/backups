package com.imranzahid.backup;

import com.imranzahid.backup.entity.Databases;
import com.imranzahid.backup.jobs.BackupDatabaseJob;
import com.imranzahid.backup.util.BackupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author imranzahid Date: 12/25/14 Time: 9:50 AM
 */
public class BackupMain {
  private static final Logger log = LoggerFactory.getLogger(BackupMain.class);

  private void init() {
    String xmlFile = System.getProperty("backups");
    if (xmlFile == null || xmlFile.isEmpty()) {
      log.error("Unable to load backups.xml file");
      return;
    }
    File file = new File(xmlFile);
    if (!file.canRead() || !file.exists()) {
      log.error("Unable to read backups.xml file");
      return;
    }

    Databases databases = BackupUtil.parseDatabaseXml();
    if (databases != null) {
      new BackupDatabaseJob().execute(databases);
    }
  }

  public static void main(String[] args) {
    new BackupMain().init();
  }
}
