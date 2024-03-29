package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import java.security.SecureRandom;
import org.apache.commons.lang3.RandomStringUtils;

public final class PasswordUtil {

  private static final int PASSWORD_LENGTH = 60;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String DIGITS = "0123456789";
  private static final String SPECIAL_CHARS = "-_,.";
  private static final char[] CHARS =
      (UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS).toCharArray();

  private PasswordUtil() {
    // utility class
  }

  public static String createPassword() {
    String password;
    do {
      password = createPasswordInternal();
    } while (!isValidPassword(password));
    return password;
  }

  private static String createPasswordInternal() {
    return RandomStringUtils.random(
        PASSWORD_LENGTH, 0, CHARS.length - 1, false, false, CHARS, SECURE_RANDOM);
  }

  // only for testing
  static boolean isValidPassword(String password) {
    return password.chars().anyMatch(c1 -> UPPER_CASE.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> LOWER_CASE.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> DIGITS.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> SPECIAL_CHARS.chars().anyMatch(c2 -> c2 == c1))
        // first character should not be a special character to avoid invalid configuration files
        && SPECIAL_CHARS.chars().noneMatch(c -> c == password.charAt(0));
  }
}
