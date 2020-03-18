package com.imranzahid.backup.entity;

import java.util.ArrayList;
import java.util.List;

public class FileFormat {
  private String template;
  private final List<FileFormatParam> params = new ArrayList<>();

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public List<FileFormatParam> getParams() {
    return params;
  }

  public void addParam(String ordinal, String pattern, String param) {
    int ord = 0;
    try { ord = Integer.parseInt(ordinal); } catch (NumberFormatException ignored) { }
    addParam(ord, pattern, param);
  }

  public void addParam(int ordinal, String pattern, String param) {
    FileFormatParam fileFormatParam = new FileFormatParam();
    fileFormatParam.setOrdinal(ordinal);
    fileFormatParam.setPattern(pattern);
    fileFormatParam.setParam(param);
    params.add(fileFormatParam);
  }
}
