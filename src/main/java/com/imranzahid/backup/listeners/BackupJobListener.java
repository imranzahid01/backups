package com.imranzahid.backup.listeners;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author imranzahid Date: 12/25/14 Time: 4:44 PM
 */
public class BackupJobListener implements JobListener {
  private static final Logger log = LoggerFactory.getLogger(BackupJobListener.class);
  private static final DateFormat sdf = new SimpleDateFormat("dd-MMM-yyy HH:mm:ss");

  @Override public String getName() {
    return BackupJobListener.class.getSimpleName();
  }

  @Override public void jobToBeExecuted(JobExecutionContext context) { }

  @Override public void jobExecutionVetoed(JobExecutionContext context) { }

  @Override public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    printDetails(context, "jobWasExecuted");
  }

  private void printDetails(JobExecutionContext context, String methodName) {
    if ("BackupConfigurationChangeListenerJob".equals(context.getJobDetail().getDescription())) {
      return;
    }
    String next = "never";
    if (context.getNextFireTime() != null) {
      next = sdf.format(context.getNextFireTime());
    }
    String msg = String.format("[%s] (%s) Executing next in: %s", methodName,
                               context.getJobDetail().getDescription(), next);
    log.info(msg);
  }
}
