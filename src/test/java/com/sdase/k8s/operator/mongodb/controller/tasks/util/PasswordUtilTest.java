package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class PasswordUtilTest {

  @RepeatedTest(1000)
  void shouldAlwaysHaveUpperCaseLetters() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual.toCharArray()).containsAnyOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
  }

  @RepeatedTest(1000)
  void shouldAlwaysHaveLowerCaseLetters() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual.toCharArray()).containsAnyOf("abcdefghijklmnopqrstuvwxyz".toCharArray());
  }

  @RepeatedTest(1000)
  void shouldAlwaysHaveDigits() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual.toCharArray()).containsAnyOf("0123456789".toCharArray());
  }

  @RepeatedTest(1000)
  void shouldAlwaysHaveSpecialCharacters() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual.toCharArray())
        .containsAnyOf("~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?".toCharArray());
  }

  @RepeatedTest(1000)
  void shouldNeverContainWhiteSpace() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual).doesNotContainAnyWhitespaces();
  }

  @RepeatedTest(1000)
  void shouldAlwaysHaveSixtyCharacters() {
    var actual = PasswordUtil.createPassword();
    assertThat(actual).hasSize(60);
  }
}
