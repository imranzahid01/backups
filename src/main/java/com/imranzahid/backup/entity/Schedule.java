package com.imranzahid.backup.entity;

/**
 * @author imranzahid Date: 12/25/14 Time: 9:56 AM
 */
public class Schedule {
  private String guid;
  private String name;
  private String cron;
  private boolean forced;
  private JobsData data;

  public Schedule(String guid) {
    this.guid = guid;
  }

  public String getGuid() {
    return guid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public boolean isForced() {
    return forced;
  }

  public void setForced(boolean forced) {
    this.forced = forced;
  }

  public JobsData getData() {
    return data;
  }

  public void setData(JobsData data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    Schedule schedule = (Schedule) o;
    return guid.equals(schedule.guid);
  }

  @Override
  public int hashCode() {
    return guid.hashCode();
  }

  @Override public String toString() {
    return "Schedule{" +
        "name='" + name + '\'' +
        ", cron='" + cron + '\'' +
        ", forced=" + forced +
        '}';
  }
}
