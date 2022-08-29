package com.imranzahid.backup.entity;

import com.imranzahid.backup.util.BackupUtil;

public class Database {
  private String name;
  private String location;
  private String compression;
  private boolean upload = true;
  private String keep;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getCompression() {
    return compression;
  }

  public void setCompression(String compression) {
    this.compression = compression;
  }

  public boolean isUpload() {
    return upload;
  }

  public void setUpload(boolean upload) {
    this.upload = upload;
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
}
