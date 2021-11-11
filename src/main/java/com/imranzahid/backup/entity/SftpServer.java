package com.imranzahid.backup.entity;

public class SftpServer {
  private String host;
  private int port;
  private String user;
  private String pass;
  private FileFormat pathFormat;

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

  public FileFormat getPathFormat() {
    return pathFormat;
  }

  public FileFormat newPathFormat() {
    this.pathFormat = new FileFormat();
    return this.pathFormat;
  }
}
