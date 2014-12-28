package com.imranzahid.backup.entity;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author imranzahid Date: 12/25/14 Time: 5:16 PM
 */
public class Databases implements JobsData {
  private static final int SECOND = 60;
  private static final int MINUTE = 60 * SECOND;
  private static final int HOUR = 60 * MINUTE;
  private static final int DAY = 24 * HOUR;
  private static final int WEEK = 7 * DAY;
  private static final int MONTH = 30 * DAY;
  private static final int YEAR = 54 * WEEK;

  public class Database {
    private String name;
    private String location;
    private String compression;
    private List<String> groupings = new ArrayList<>();

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

    public List<String> getGroupings() {
      Optional<String> none = Iterables.tryFind(groupings, new Predicate<String>() {
        @Override public boolean apply(String input) {
          return Objects.requireNonNull(input, "Grouping cannot be null").equalsIgnoreCase("NONE");
        }
      });
      if (none.isPresent()) {
        return Collections.emptyList();
      }
      return groupings;
    }

    public boolean groupingsUsed() {
      return groupings.size() > 0;
    }
  }

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

  public class FileFormat {
    private String template;
    private List<FileFormatParam> params = new ArrayList<>();

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

  public class Server {
    private String host;
    private int port;
    private String instance;
    private String user;
    private String pass;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public void setPort(String port) {
      try { setPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) { }
    }

    public String getInstance() {
      return instance;
    }

    public void setInstance(String instance) {
      this.instance = instance;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPass() {
      return pass;
    }

    public void setPass(String pass) {
      this.pass = pass;
    }
  }

  private String name;
  private String base;
  private String cron;
  private FileFormat fileFormat;
  private List<String> groupings = new ArrayList<>();
  private String keep;
  private List<String> emails = new ArrayList<>();
  private Server server;
  private List<Database> databases = new ArrayList<>();

  @Override public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBase() {
    return base;
  }

  public void setBase(String base) {
    this.base = base;
  }

  @Override public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public FileFormat getFileFormat() {
    return fileFormat;
  }

  public FileFormat newFileFormat() {
    this.fileFormat = new FileFormat();
    return this.fileFormat;
  }

  public List<String> getGroupings() {
    Optional<String> none = Iterables.tryFind(groupings, new Predicate<String>() {
      @Override public boolean apply(String input) {
        return Objects.requireNonNull(input, "Grouping cannot be null").equalsIgnoreCase("NONE");
      }
    });
    if (none.isPresent()) {
      return Collections.emptyList();
    }
    return groupings;
  }

  public boolean groupingsUsed() {
    return groupings.size() > 0;
  }

  public String getKeep() {
    return keep;
  }

  public void setKeep(String keep) {
    this.keep = keep;
  }

  public long getKeepFor() {
    if (Strings.isNullOrEmpty(getKeep())) {
      return -1;
    }
    StringBuilder number = new StringBuilder();
    StringBuilder unit = new StringBuilder();
    for (int i = 0; i < getKeep().length(); i++) {
      char ch = getKeep().charAt(i);
      if (Character.isDigit(ch)) {
        number.append(ch);
      }
      else if (Character.isLetter(ch)) {
        unit.append(ch);
      }
    }
    try {
      int num = Integer.parseInt(number.toString());
      switch(unit.toString()) {
        case "S": case "s": return num * DAY;
        case "m":           return num * MINUTE;
        case "h": case "H": return num * HOUR;
        case "D": case "d": return num * DAY;
        case "w": case "W": return num * WEEK;
        case "M":           return num * MONTH;
        case "y": case "Y": return num * YEAR;
      }
    } catch (NumberFormatException ignored) { }
    return -1;
  }

  @Override public List<String> getEmails() {
    return emails;
  }

  public Server getServer() {
    return server;
  }

  public Server newServer() {
    this.server = new Server();
    return this.server;
  }

  public List<Database> getDatabases() {
    return databases;
  }

  public Database newDatabase(String name) {
    Database database = new Database();
    database.setName(name);
    return database;
  }
}
