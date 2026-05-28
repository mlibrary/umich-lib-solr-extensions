// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a Library of Congress (LC) call number into its structural components and produces a
 * sortable collation key.
 *
 * <p>A valid LC call number consists of 1-3 classification letters (one or two Unicode letters, a
 * {@code K}-prefixed law string of up to 3 letters, or the literal string {@code LAW}), followed by
 * 1-5 class digits, an optional decimal subdivision, and arbitrary trailing text.
 *
 * <p>The collation key encodes the digit portion with a length prefix so that, for example, {@code
 * QA9} sorts before {@code QA10}.
 */
public class LCCallNumberSimple extends AbstractCallNumber {

  public String letters = "";
  public String digits = "";
  public String decimals = "";
  public String rest = "";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // THe letters for a callnumber are either one letter, two letters, a K followed by 0-2 letters,
  // or
  // the three letter sequence LAW

  public static String LETTER_PAT = "(?<letters>(?:LAW|law|Law|[Kk]\\p{L}{0,2}|\\p{L}{1,2}))";

  public static Pattern LC_START =
      Pattern.compile(
          "^\\s*"
              + LETTER_PAT
              + "\\s*"
              + "(?<digits>\\d{1,5}(?!\\d))"
              + // 1-5 digits
              "(?:\\.(?<decimals>\\d+))?"
              + // an optional decimal ('.' plus digits)
              "(?<rest>.*)$" // Whatever's left
          );

  // When searching, we'll often want a range query that starts with only letters
  // That can be a single letter, any two-letter combination, or a set of three
  // letters starting with "K" (books about legal issues) or "L" (more of the same).
  // @TODO Put a guard around letter-only queries so we only accept them when an argument to the
  // constructor says to.

  public static Pattern ACCEPTABLE_ONLY_LETTERS = Pattern.compile(LETTER_PAT + "\\s*$");

  public LCCallNumberSimple(String str) {
    original = str;
    trimmedOriginal = trimPunctuation(str.trim()).trim().toLowerCase();
    Matcher m = LC_START.matcher(trimmedOriginal);
    if (m.matches()) {
      isValid = true;
      letters = m.group("letters");
      digits = m.group("digits");
      decimals = m.group("decimals");
      rest = m.group("rest");
    } else {
      LOGGER.debug("LC Callnumber '" + trimmedOriginal + "' is invalid.");
      isValid = false;
    }
    if (hasAcceptableTruncatedKey()) {
      LOGGER.debug("Original '" + trimmedOriginal + "'matches one-ACCEPTABLE_ONLY pattern");
    }
  }

  /**
   * @return {@code true} if the input matched the LC call-number pattern
   */
  @Override
  public Boolean hasValidKey() {
    return isValid;
  }

  /**
   * @return the sortable collation key, or {@code null} if the parse failed
   */
  @Override
  public String validKey() {
    return collationKey();
  }

  /**
   * Builds the sortable collation key as: {@code letters + len(digits) + digits [+ "." + decimals]
   * [+ " " + rest]}. The length-prefix on the digit segment ensures lexicographic order matches
   * numeric order (e.g. {@code QA9} &lt; {@code QA10}).
   *
   * @return the collation key, or {@code null} if the call number is invalid
   */
  public String collationKey() {
    if (isValid) {
      String key = collationLetters() + collationDigits() + collationDecimals() + collationRest();
      return collapseSpaces(key);
    } else {
      return null;
    }
  }

  /**
   * @return {@code true} if the input consists solely of LC classification letters (optionally
   *     surrounded by whitespace), making it a valid prefix for a range query such as {@code [QA TO
   *     QB]}
   */
  @Override
  public Boolean hasAcceptableTruncatedKey() {
    return isAcceptableTruncatedCallnumber(original);
  }

  /**
   * @return the trimmed, lower-cased original string when it is an acceptable letter-only truncated
   *     query; otherwise {@code null}
   */
  @Override
  public String acceptableTruncatedKey() {
    if (hasAcceptableTruncatedKey()) {
      return trimmedOriginal;
    } else {
      return null;
    }
  }

  /**
   * @return a cleaned-up freetext representation of the call number suitable for sorting
   *     unrecognised strings alongside valid ones
   */
  @Override
  public String invalidKey() {
    return cleanupFreetext(trimmedOriginal);
  }

  private String collationLetters() {
    return letters;
  }

  private String collationDigits() {
    Integer digitLength = digits.length();
    return digitLength + digits;
  }

  private String collationDecimals() {
    if (decimals == null) {
      return "";
    } else {
      return "." + decimals;
    }
  }

  private String collationRest() {
    if ((rest == null) || (rest.equals(""))) {
      return "";
    } else {
      return " " + cleanupFreetext(rest);
    }
  }

  private String cleanupFreetext(String str) {
    if (str == null) return "";
    String s = str.trim();
    if (s.equals("")) {
      return s;
    }
    String rv = replaceDotBeforeLetterWithSpace(s);
    rv = removeDotsBetweenLetters(rv);
    rv = removeNonDecimalPointPunctuation(rv);
    rv = forceSpaceBetweenDigitAndLetter(rv);
    return collapseSpaces(rv);
  }

  private Boolean isAcceptableTruncatedCallnumber(String str) {
    return ACCEPTABLE_ONLY_LETTERS.matcher(str.trim().toLowerCase()).matches();
  }

  private String removeDotsBetweenLetters(String str) {
    return str.replaceAll("(\\p{L})\\.(\\p{L})", "$1$2");
  }

  private String replaceDotBeforeLetterWithSpace(String str) {
    return str.replaceAll("\\s+\\.(\\p{L})", " $1");
  }

  private String removeNonDecimalPointPunctuation(String str) {
    return str.replaceAll("(\\d)\\.(\\d)", "$1AAAAA$2")
        .replaceAll("\\p{P}", "")
        .replaceAll("(\\d)AAAAA(\\d)", "$1.$2");
  }

  private String forceSpaceBetweenDigitAndLetter(String str) {
    return str.replaceAll("(\\d)(\\p{L})", "$1 $2");
  }
}
