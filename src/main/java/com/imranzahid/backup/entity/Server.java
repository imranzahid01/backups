package com.imranzahid.backup.entity;

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
