package com.imranzahid.backup.util;

import com.imranzahid.backup.entity.SftpServer;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SftpClient {
  private static final Logger log = LoggerFactory.getLogger(SftpClient.class);
  private final SftpServer sftpServer;
  private ChannelSftp channelSftp;

  public SftpClient(@Nonnull SftpServer sftpServer) {
    this.sftpServer = sftpServer;
  }

  public boolean connect() {
    if (channelSftp != null) {
      return true;
    }
    try {
      JSch jsch = new JSch();
      Session jschSession = jsch.getSession(sftpServer.getUser(), sftpServer.getHost());
      if (sftpServer.getPort() > 0) {
        jschSession.setPort(sftpServer.getPort());
      }
      jschSession.setPassword(sftpServer.getPass());
      jschSession.setConfig("StrictHostKeyChecking", "no");
      jschSession.connect();
      channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
      channelSftp.connect();
    } catch (JSchException e) {
      log.error("Unable to connect", e);
    }
    return channelSftp != null;
  }

  public boolean mkdirs(String path) {
    log.debug("mkdirs: " + path);
    try {
      String[] folders = path.split("/");
      folders[0] = (path.charAt(0) != '/' ? channelSftp.pwd() + "/" : "") + folders[0];

      for (String f : folders) {
        if (f.length() == 0) {
          continue;
        }
        if (!folderExists(f)) {
          log.debug("\tmkdir: " + f);
          channelSftp.mkdir(f);
        }
        log.debug("\tcd: " + f);
        channelSftp.cd(f);
      }
      return true;
    } catch (SftpException | ArrayIndexOutOfBoundsException e) {
      log.error("Unable to create dirs: " + path, e);
    }
    return false;
  }

  public boolean folderExists(String folderName) {
    try {
      SftpATTRS attrs = channelSftp.stat(folderName);
      if (attrs == null || ((attrs.getFlags() & SftpATTRS.SSH_FILEXFER_ATTR_PERMISSIONS ) == 0)) {
        return false;
      }
      return attrs.isDir();
    } catch (SftpException e) {
      if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
        return false;
      }
      log.error("Unable to determine if folder (" + folderName + ") exists", e);
    }
    return false;
  }

  public boolean put(String name, File file) {
    try(FileInputStream fs = new FileInputStream(file)) {
      channelSftp.put(fs, name, ChannelSftp.OVERWRITE);
      return true;
    } catch (SftpException | IOException e) {
      log.error("Unable to upload file " + name, e);
    }
    return false;
  }
}
