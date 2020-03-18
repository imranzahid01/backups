package com.imranzahid.backup.entity;

public class FileFormatParam {
  private int ordinal;
  private String pattern;
  private String param;

  public int getOrdinal() {
    return ordinal;
  }

  public void setOrdinal(int ordinal) {
    this.ordinal = ordinal;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getParam() {
    return param;
  }

  public void setParam(String param) {
    this.param = param;
  }
}
