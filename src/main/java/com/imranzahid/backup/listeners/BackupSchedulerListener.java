package com.imranzahid.backup.listeners;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author imranzahid Date: 12/25/14 Time: 4:45 PM
 */
public class BackupSchedulerListener implements SchedulerListener {
  private static final Logger log = LoggerFactory.getLogger(BackupSchedulerListener.class);
  private static final DateFormat sdf = new SimpleDateFormat("dd-MMM-yyy HH:mm:ss");

  @Override public void jobScheduled(Trigger trigger) {
    log.info(String.format("[jobScheduled] Trigger(%s, %s, %s) next executing in: %s",
                           trigger.getKey().getName(), trigger.getKey().getGroup(),
                           trigger.getJobDataMap().getString("cron"),
                           sdf.format(trigger.getNextFireTime())));
  }

  @Override public void jobUnscheduled(TriggerKey triggerKey) { }
  @Override public void triggerFinalized(Trigger trigger) { }
  @Override public void triggerPaused(TriggerKey triggerKey) { }
  @Override public void triggersPaused(String triggerGroup) { }
  @Override public void triggerResumed(TriggerKey triggerKey) { }
  @Override public void triggersResumed(String triggerGroup) { }

  @Override public void jobAdded(JobDetail jobDetail) {
    /*log.info(String.format("[jobAdded] JobDetail(%s, %s)",
                           jobDetail.getKey().getName(), jobDetail.getKey().getGroup()));*/
  }

  @Override public void jobDeleted(JobKey jobKey) { }
  @Override public void jobPaused(JobKey jobKey) { }
  @Override public void jobsPaused(String jobGroup) { }
  @Override public void jobResumed(JobKey jobKey) { }
  @Override public void jobsResumed(String jobGroup) { }
  @Override public void schedulerError(String msg, SchedulerException cause) { }
  @Override public void schedulerInStandbyMode() { }
  @Override public void schedulerStarted() { }
  @Override public void schedulerStarting() { }
  @Override public void schedulerShutdown() { }
  @Override public void schedulerShuttingdown() { }
  @Override public void schedulingDataCleared() { }
}
