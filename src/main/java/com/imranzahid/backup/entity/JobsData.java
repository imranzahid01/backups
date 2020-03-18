package com.imranzahid.backup.entity;

import java.util.List;

/**
 * @author imranzahid Date: 12/25/14 Time: 7:51 PM
 */
public interface JobsData {
  String getName();
  String getCron();
  List<String> getEmails();
  String getHealthCheckUuid();
}
