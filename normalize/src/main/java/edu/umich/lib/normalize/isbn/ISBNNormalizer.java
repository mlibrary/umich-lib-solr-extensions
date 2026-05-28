// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.isbn;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting and normalizing ISBN strings.
 *
 * <p>All public methods return {@code null} when the input does not contain a recognizable ISBN,
 * rather than throwing. A DEBUG-level log message is emitted for diagnostic purposes when
 * extraction fails.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public final class ISBNNormalizer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Utility class — do not instantiate. */
  private ISBNNormalizer() {}

  private static final String ISBN_DELIMITER_PATTERN = "\\-";

  public static final Pattern ISBN10Pattern =
      Pattern.compile("^.*?(\\d[\\d\\-]{8,}[Xx]?)(?:\\D|\\Z).*$");

  public static final Pattern ISBN13Pattern =
      Pattern.compile("^.*?(97[89][\\d\\-]{10,})(?:\\D|\\Z).*$");

  /**
   * Try to extract an ISBN from the string. ISBN-13s are returned as-is; ISBN-10s are converted to
   * ISBN-13 and returned.
   *
   * @param isbnstring the string that may contain an ISBN
   * @return a normalized ISBN-13 string, or {@code null} if no ISBN was found
   */
  public static String normalize(String isbnstring) {
    String isbn13 = extractIsbn13(isbnstring);
    if (isbn13 != null) {
      return isbn13;
    }
    String isbn10 = extractIsbn10(isbnstring);
    if (isbn10 != null) {
      return isbn10To13(isbn10);
    }
    LOGGER.debug("No ISBN found in: {}", isbnstring);
    return null;
  }

  /**
   * Extracts an ISBN from {@code isbnstring} using the given pattern and expected digit length.
   *
   * @param isbnstring the input string
   * @param pat the pattern to match against
   * @param len the expected digit count (10 or 13)
   * @return the extracted digits, or {@code null} if extraction failed
   */
  public static String extractIsbnByPat(String isbnstring, Pattern pat, int len) {
    Matcher m = pat.matcher(isbnstring);
    if (!m.matches()) {
      LOGGER.debug("{} doesn't match ISBN{} pattern", isbnstring, len);
      return null;
    }

    String extracted = m.group(1);
    String normalized = extracted.replaceAll(ISBN_DELIMITER_PATTERN, "");

    if (normalized.length() != len) {
      LOGGER.debug(
          "'{}' matched ISBN{} pattern but has wrong length: {}",
          normalized,
          len,
          normalized.length());
      return null;
    }
    return normalized;
  }

  /**
   * Extracts an ISBN-10 digit string from {@code isbnstring}.
   *
   * @return the 10-digit string, or {@code null} if not found
   */
  public static String extractIsbn10(String isbnstring) {
    return extractIsbnByPat(isbnstring, ISBN10Pattern, 10);
  }

  /**
   * Extracts an ISBN-13 digit string from {@code isbnstring}.
   *
   * @return the 13-digit string, or {@code null} if not found
   */
  public static String extractIsbn13(String isbnstring) {
    return extractIsbnByPat(isbnstring, ISBN13Pattern, 13);
  }

  /**
   * Converts an already-extracted ISBN-10 digit string to an ISBN-13.
   *
   * @param isbn10 the raw digits (plus possible trailing 'X') of an ISBN-10
   * @return the equivalent ISBN-13
   */
  public static String isbn10To13(String isbn10) {
    String longisbn = "978" + isbn10.substring(0, 9);

    int[] digits = new int[12];
    for (int i = 0; i < 12; i++) {
      digits[i] = Integer.parseInt(longisbn.substring(i, i + 1));
    }

    int sum = 0;
    for (int i = 0; i < 12; i++) {
      sum = sum + digits[i] + (2 * digits[i] * (i % 2));
    }

    // Get the smallest multiple of ten >= sum, then compute check digit
    int check = 10 - (sum % 10);
    if (check == 10) {
      return longisbn + "0";
    } else {
      return longisbn + check;
    }
  }
}
