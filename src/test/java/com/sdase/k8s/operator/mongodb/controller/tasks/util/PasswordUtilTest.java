package com.sdase.k8s.operator.mongodb.controller.tasks.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordUtilTest {

  private static final String SPECIAL_CHARS = "-_,.";

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
    assertThat(actual.toCharArray()).containsAnyOf(SPECIAL_CHARS.toCharArray());
  }

  @RepeatedTest(1000)
  void shouldNeverContainSpecialCharacterAtTheBeginning() {
    var actual = PasswordUtil.createPassword();
    assertThat(Character.toString(actual.charAt(0))).doesNotContain(SPECIAL_CHARS);
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

  @RepeatedTest(1000)
  void shouldNotNeedEscapingInConnectionString() {
    // sda-commons does not escape username and password
    var actual = PasswordUtil.createPassword();

    assertThatNoException()
        .as("Password %s should not throw exception", actual)
        .isThrownBy(
            () -> new ConnectionString("mongodb://username:foo_" + actual + "_bar@mongodb/my-db"));
  }

  @ParameterizedTest
  @ValueSource(chars = {'-', '_', ',', '.'})
  void shouldNotAcceptPasswordWithSpecialCharacterAtTheBeginning(char specialChar) {
    String given = PasswordUtil.createPassword();
    given = "" + specialChar + given.substring(1);

    var actual = PasswordUtil.isValidPassword(given);

    assertThat(actual).isFalse();
  }
}
