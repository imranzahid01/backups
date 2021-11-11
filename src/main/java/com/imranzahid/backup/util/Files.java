package com.imranzahid.backup.util;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Files {
  private Files() {}

  public static void createParentDirs(@Nonnull File file) throws IOException {
    Objects.requireNonNull(file);
    File parent = file.getCanonicalFile().getParentFile();
    if (parent == null) {
      return;
    }
    //noinspection ResultOfMethodCallIgnored
    parent.mkdirs();
    if (!parent.isDirectory()) {
      throw new IOException("Unable to create parent directories of " + file);
    }
  }
}
