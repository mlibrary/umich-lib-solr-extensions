// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For purposes of this simple normalizer, define a valid dewey number as follows: * Starts with
 * three digits * Optional decimal places which can include IGNORED slashes or apostrophes *
 * Whatever's left: * Lowercase * remove dots after a letter * Trim spaces * remove leading/trailing
 * punctuation (which will take care of a dot before a cutter) * trim spaces again * compact spaces
 * * add back in with a preceding space
 */
public class DeweySimple extends AbstractCallNumber {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public String digits = "";
  public String decimals = "";
  public String rest = "";

  public static Pattern DEWEY_PATTERN =
      Pattern.compile(
          "^\\s*(?<digits>\\d{3})"
              + "\\s*"
              + "(?:\\.(?<decimals>[\\d/']+))?"
              + "(?:\\s+(?<rest>.*))?$");

  public static Pattern ACCEPTABLE_THREE_DIGITS_PATTERN = Pattern.compile("^\\s*\\d{3}\\s*$");

  public DeweySimple(String str) {
    trimmedOriginal = trimPunctuation(str.trim().toLowerCase());
    Matcher m = DEWEY_PATTERN.matcher(trimmedOriginal);
    if (m.matches()) {
      isValid = true;
      digits = m.group("digits");
      decimals = fixedDecimals(m.group("decimals"));
      rest = cleanupFreetext(m.group("rest"));
    } else {
      LOGGER.debug(trimmedOriginal + " is invalid");
      isValid = false;
    }
  }

  /**
   * @return {@code true} if the input matched the three-digit Dewey pattern
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
   * Builds the sortable collation key as {@code trimPunctuation(digits + decimals + rest)}.
   *
   * @return the collation key, or {@code null} if the call number is invalid
   */
  public String collationKey() {
    if (isValid) {
      return trimPunctuation(digits + decimals + rest);
    } else {
      return null;
    }
  }

  /**
   * @return {@code true} if the input is exactly three digits (optionally surrounded by
   *     whitespace), making it a valid prefix for a range query (e.g. {@code [500 TO 600]})
   */
  @Override
  public Boolean hasAcceptableTruncatedKey() {
    return isValidTruncatedQuery(trimmedOriginal);
  }

  /**
   * @return the trimmed, lower-cased original string when it is a valid three-digit truncated
   *     query; otherwise {@code null}
   */
  @Override
  public String acceptableTruncatedKey() {
    if (isValidTruncatedQuery(trimmedOriginal)) {
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

  private String fixedDecimals(String str) {
    if (str == null) return "";
    if (str.trim().equals("")) return "";
    return "." + str.trim().replaceAll("[/']+", "");
  }

  private String cleanupFreetext(String str) {
    if (str == null) return "";
    String s = str.trim();
    if (s.equals("")) {
      return s;
    }
    s = removeDotsBetweenLetters(s);
    s = ditchDotsAfterLetters(s);
    s = trimPunctuation(s);
    s = collapseSpaces(s);
    s = " " + s;
    return s;
  }

  private String removeDotsBetweenLetters(String str) {
    return str.replaceAll("(\\p{L})\\.(\\p{L})", "$1$2");
  }

  /**
   * Replaces a dot that immediately follows a letter with a space, removing abbreviated-name dots
   * that should not be treated as decimal points (e.g. {@code "M.A."} becomes {@code "M A "}).
   *
   * @param str the string to process
   * @return the string with letter-dots replaced by spaces
   */
  public String ditchDotsAfterLetters(String str) {
    return str.replaceAll("(\\p{L})\\.", "$1 ");
  }

  private Boolean isValidTruncatedQuery(String str) {
    return ACCEPTABLE_THREE_DIGITS_PATTERN.matcher(str).matches();
  }
}
