package com.imranzahid.backup.util;

import javax.annotation.Nullable;

public final class Strings {
  private Strings() {}

  public static boolean isNullOrEmpty(@Nullable String string) {
    return string == null || string.isEmpty();
  }
}
