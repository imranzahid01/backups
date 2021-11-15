package com.imranzahid.backup.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Strings {
  private Strings() {}

  public static boolean isNullOrEmpty(@Nullable String string) {
    return string == null || string.isEmpty();
  }

  public static String replace(@Nonnull String input, @Nonnull CharSequence[] target,
                               @Nonnull CharSequence[] replacement) {
    String output = input;
    for (int i = 0; i < target.length; i++) {
      output = output.replace(target[i], replacement[i]);
    }
    return output;
  }
}
