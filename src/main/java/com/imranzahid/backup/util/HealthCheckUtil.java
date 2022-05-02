package com.imranzahid.backup.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

public class HealthCheckUtil implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(HealthCheckUtil.class);
  private static final String BASE_URL = System.getProperty("hc.url", "https://hc.btm-binds.com/ping/");
  private static final OkHttpClient httpClient = new OkHttpClient();

  @Nonnull public static HealthCheckUtil getInstance(@Nullable String uuid) {
    return new HealthCheckUtil(uuid);
  }

  @Nonnull private final String uuid;
  private HealthCheckUtil(@Nullable String uuid) {
    this.uuid = (uuid == null ? "" : uuid);
  }

  @Nonnull private String getUrl() {
    return BASE_URL + uuid;
  }

  public void start() {
    if (uuid.isBlank()) {
      return;
    }
    execute(new Request.Builder()
      .url(getUrl() + "/start")
      .build());
  }

  public void success(@Nullable String message) {
    if (uuid.isBlank()) {
      return;
    }
    Request.Builder request = new Request.Builder().url(getUrl());
    if (message != null && !message.isBlank()) {
      request.post(RequestBody.create(message, MediaType.parse("text/plain")));
    }
    execute(request.build());
  }

  public void fail(@Nullable String message) {
    if (uuid.isBlank()) {
      return;
    }
    Request.Builder request = new Request.Builder().url(getUrl() + "/fail");
    if (message != null && !message.isBlank()) {
      request.post(RequestBody.create(message, MediaType.parse("text/plain")));
    }
    execute(request.build());
  }

  private void execute(@Nonnull Request request) {
    try {
      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.error("Invalid response from httpCall : " + response);
        }
        ResponseBody body = response.body();
        if (body == null) {
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
