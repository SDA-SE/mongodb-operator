package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import java.security.SecureRandom;
import org.apache.commons.lang3.RandomStringUtils;

public class PasswordUtil {

  private static final int PASSWORD_LENGTH = 60;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String DIGITS = "0123456789";
  // FIXME HPC-906 some of these characters can't be handled by sda-commons test and bundle
  private static final String SPECIAL_CHARS = "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
  private static final char[] CHARS =
      (UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS).toCharArray();

  private PasswordUtil() {
    // utility class
  }

  public static String createPassword() {
    String password;
    do {
      password = createPasswordInternal();
    } while (!containsAllTypeOfCharacters(password));
    return password;
  }

  private static String createPasswordInternal() {
    return RandomStringUtils.random(
        PASSWORD_LENGTH, 0, CHARS.length - 1, false, false, CHARS, SECURE_RANDOM);
  }

  private static boolean containsAllTypeOfCharacters(String password) {
    return password.chars().anyMatch(c1 -> UPPER_CASE.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> LOWER_CASE.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> DIGITS.chars().anyMatch(c2 -> c2 == c1))
        && password.chars().anyMatch(c1 -> SPECIAL_CHARS.chars().anyMatch(c2 -> c2 == c1));
  }
}
