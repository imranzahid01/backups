package com.imranzahid.backup.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class HealthCheckUtil implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(HealthCheckUtil.class);
  private static final String BASE_URL = "https://hc-ping.com/";
  private static final OkHttpClient httpClient = new OkHttpClient();

  public static HealthCheckUtil getInstance(String uuid) {
    return new HealthCheckUtil(uuid);
  }

  private final String uuid;
  private HealthCheckUtil(String uuid) {
    this.uuid = uuid;
  }

  private String getUrl() {
    return BASE_URL + uuid;
  }

  public void start() {
    if (uuid == null || uuid.isBlank()) {
      return;
    }
    Request request = new Request.Builder()
      .url(getUrl() + "/start").build();
    execute(request);
  }

  public void success(String message) {
    if (uuid == null || uuid.isBlank()) {
      return;
    }
    Request.Builder request = new Request.Builder().url(getUrl());
    if (message != null && !message.isBlank()) {
      request.post(RequestBody.create(message, MediaType.parse("text/plain")));
    }
    execute(request.build());
  }

  public void fail(String message) {
    if (uuid == null || uuid.isBlank()) {
      return;
    }
    Request.Builder request = new Request.Builder().url(getUrl() + "/fail");
    if (message != null && !message.isBlank()) {
      request.post(RequestBody.create(message, MediaType.parse("text/plain")));
    }
    execute(request.build());
  }

  private void execute(Request request) {
    try {
      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.error("Invalid response from httpCall : " + response);
        }
        ResponseBody body = response.body();
        if (body != null) {
          log.info(body.string());
        }
        else {
          log.error("Response body is null");
        }
      }
    }
    catch (Exception ex) {
      log.error("Unable to execute httpCall", ex);
    }
  }

  @Override public void close() throws IOException {
    httpClient.dispatcher().executorService().shutdown();
    httpClient.connectionPool().evictAll();
    if (httpClient.cache() != null) {
      httpClient.cache().close();
    }
  }
}
