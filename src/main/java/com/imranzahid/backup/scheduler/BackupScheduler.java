package com.imranzahid.backup.scheduler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.imranzahid.backup.entity.*;
import com.imranzahid.backup.jobs.BackupDatabaseJob;
import com.imranzahid.backup.listeners.BackupJobListener;
import com.imranzahid.backup.listeners.BackupSchedulerListener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

/**
 * @author imranzahid Date: 12/25/14 Time: 9:54 AM
 */
public class BackupScheduler {
  private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);
  private boolean init = false;
  private static BackupScheduler instance;

  private BackupScheduler() {
    try {
      log.info("Initializing scheduler");
      Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
      scheduler.start();
      scheduler.getListenerManager().addJobListener(new BackupJobListener(), jobGroupEquals("backup"));
      scheduler.getListenerManager().addSchedulerListener(new BackupSchedulerListener());
      init = true;
    }
    catch (SchedulerException se) {
      log.error("Unable to initialize scheduler");
    }
  }

  public static BackupScheduler getInstance() {
    if (instance == null) {
      instance = new BackupScheduler();
    }
    return instance;
  }

  public boolean hasInit() {
    return init;
  }

  public void startScheduler() {
    ImmutableMap<ScheduleType, Schedule> schedules = null;
    try {
      Databases databases = null;
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
        parseDatabasesJob(databases, databasesElement);
      }
      ImmutableMap.Builder<ScheduleType, Schedule> scheduleBuilder = new ImmutableMap.Builder<>();
      if (databases != null) {
        Schedule schedule = new Schedule(UUID.randomUUID().toString());
        schedule.setName(databases.getName());
        schedule.setCron(databases.getCron());
        schedule.setForced(false);
        schedule.setData(databases);
        scheduleBuilder.put(ScheduleType.DATABASE, schedule);
      }
      schedules = Maps.immutableEnumMap(scheduleBuilder.build());
    }
    catch (JDOMException | IOException e) {
      log.error("Unable to get scheduling information", e);
    }
    if (schedules != null) {
      schedule(BackupDatabaseJob.class, schedules.get(ScheduleType.DATABASE));
    }
  }

  private void parseDatabasesJob(Databases databases, Element databasesElement) {
    Element metaElement = databasesElement.getChild("meta");
    databases.setName(metaElement.getChildTextNormalize("name"));
    databases.setBase(metaElement.getChildTextNormalize("base"));
    databases.setCron(metaElement.getChildTextNormalize("cron"));
    Element fileformatElement = metaElement.getChild("fileformat");
    FileFormat fileFormat = databases.newFileFormat();
    fileFormat.setTemplate(fileformatElement.getChildTextNormalize("template"));
    Element paramsElement = fileformatElement.getChild("params");
    if (paramsElement != null) {
      List<Element> params = paramsElement.getChildren("param");
      if (params != null) {
        for (Element param : params) {
          fileFormat.addParam(param.getAttributeValue("ordinal", "0"),
                              param.getAttributeValue("pattern", ""),
                              param.getTextNormalize());
        }
      }
    }
    parseGroupings(databases, metaElement.getChild("grouping"));
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
    Server server = databases.newServer();
    server.setHost(serverElement.getChildTextNormalize("host"));
    server.setPort(serverElement.getChildTextNormalize("port"));
    server.setInstance(serverElement.getChildTextNormalize("instance"));
    server.setUser(serverElement.getChildTextNormalize("user"));
    server.setPass(serverElement.getChildTextNormalize("password"));

    List<Element> databaseElements = databasesElement.getChildren("database");
    if (databaseElements != null) {
      for (Element databaseElement : databaseElements) {
        Database database = databases.newDatabase(databaseElement.getChildTextNormalize("name"));
        database.setLocation(databaseElement.getChildTextNormalize("location"));
        database.setCompression(databaseElement.getChildTextNormalize("compression"));
        parseGroupings(database, databaseElement.getChild("grouping"));
        databases.getDatabases().add(database);
      }
    }
  }

  private void parseGroupings(Groupings entity, Element groupingElement) {
    if (groupingElement != null) {
      List<Element> groups = groupingElement.getChildren("group");
      if (groups != null) {
        for (Element group : groups) {
          entity.getGroupings().add(group.getTextNormalize());
        }
      }
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void schedule(Class<? extends Job> clazz, @Nullable Schedule schedule) {
    if (schedule == null) {
      return;
    }
    Trigger trigger = createTrigger(clazz, schedule);
    if (trigger != null && schedule.isForced()) {
      throw new RuntimeException("Forced Start not implemented yet");
    }
  }

  @Nullable
  private Trigger createTrigger(@Nonnull Class<? extends Job> clazz, @Nonnull Schedule schedule) {
    if (schedule.getCron() == null) {
      return null;
    }
    String jobName = clazz.getSimpleName();
    try {
      String triggerName = jobName + "Trigger";
      Trigger backupTrigger = newTrigger()
          .withIdentity(triggerName, "backup")
          .withDescription(triggerName)
          .withSchedule(cronSchedule(schedule.getCron()))
          .build();
      backupTrigger.getJobDataMap().put("guid", schedule.getGuid());
      backupTrigger.getJobDataMap().put("cron", schedule.getCron());
      backupTrigger.getJobDataMap().put("jobsData", schedule.getData());

      Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

      Trigger oldTrigger = scheduler.getTrigger(backupTrigger.getKey());
      if (oldTrigger == null) {
        JobDetail crimsonJob = newJob(clazz)
            .withIdentity(jobName, "backup")
            .withDescription(jobName)
            .build();
        scheduler.scheduleJob(crimsonJob, backupTrigger);
        return backupTrigger;
      }
      String oldCron = oldTrigger.getJobDataMap().getString("cron");
      if (!oldCron.equals(schedule.getCron())) {
        log.info(String.format("Rescheduling job %s from cron (%s) to (%s)", jobName, oldCron, schedule.getCron()));
        scheduler.rescheduleJob(backupTrigger.getKey(), backupTrigger);
        return backupTrigger;
      }
      return oldTrigger;
    }
    catch (RuntimeException | SchedulerException e) {
      log.error("Unable to schedule a job for " + jobName, e);
    }
    return null;
  }
}
