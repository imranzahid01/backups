package com.imranzahid.backup.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Database implements Groupings {
  private String name;
  private String location;
  private String compression;
  private final List<String> groupings = new ArrayList<>();

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

  @Override public List<String> getGroupings() {
    Optional<String> none = groupings.stream().filter(g -> g != null && g.equalsIgnoreCase("NONE")).findFirst();
    if (none.isPresent()) {
      return Collections.emptyList();
    }
    return groupings;
  }

  public boolean groupingsUsed() {
    return groupings.size() > 0;
  }
}
