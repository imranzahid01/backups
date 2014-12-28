package com.imranzahid.backup.entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author imranzahid Date: 12/25/14 Time: 9:57 AM
 */
public enum ScheduleType {
  DATABASE,
  FOLDER,
  DEBUG;

  @Nullable public static ScheduleType valueByName(@Nonnull String name) {
    for (ScheduleType scheduleType : values()) {
      if (scheduleType.name().equalsIgnoreCase(name)) {
        return scheduleType;
      }
    }
    return null;
  }
}
