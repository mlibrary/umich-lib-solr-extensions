// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.isbn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ISBNNormalizer")
class ISBNNormalizerTest {

  // -------------------------------------------------------------------------
  // normalize()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("normalize()")
  class Normalize {

    @Test
    @DisplayName("ISBN-13 without dashes is returned as-is")
    void isbn13_noDashes_returnedAsIs() {
      assertEquals("9780306406157", ISBNNormalizer.normalize("9780306406157"));
    }

    @Test
    @DisplayName("ISBN-13 with dashes has dashes stripped")
    void isbn13_withDashes_dashesStripped() {
      assertEquals("9780306406157", ISBNNormalizer.normalize("978-0-306-40615-7"));
    }

    @Test
    @DisplayName("ISBN-10 without dashes is converted to ISBN-13")
    void isbn10_noDashes_convertedToIsbn13() {
      assertEquals("9780306406157", ISBNNormalizer.normalize("0306406152"));
    }

    @Test
    @DisplayName("ISBN-10 with dashes is converted to ISBN-13")
    void isbn10_withDashes_convertedToIsbn13() {
      assertEquals("9780306406157", ISBNNormalizer.normalize("0-306-40615-2"));
    }

    @Test
    @DisplayName("ISBN-10 embedded in extraneous text is extracted and converted")
    void isbn10_embeddedInText_extractedAndConverted() {
      assertEquals("9780306406157", ISBNNormalizer.normalize("ISBN 0-306-40615-2 (hardcover)"));
    }

    @Test
    @DisplayName("Non-ISBN string returns null")
    void nonIsbn_returnsNull() {
      assertNull(ISBNNormalizer.normalize("notanisbn"));
    }

    @Test
    @DisplayName(
        "String with digits that match pattern but wrong length after stripping returns null")
    void wrongLengthAfterStripping_returnsNull() {
      // 12 digits after stripping — not a valid ISBN-13 or ISBN-10
      assertNull(ISBNNormalizer.normalize("978-0-306-40615"));
    }
  }

  // -------------------------------------------------------------------------
  // extractIsbn13() / extractIsbn10()
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("extractIsbn13()")
  class ExtractIsbn13 {

    @Test
    @DisplayName("Valid ISBN-13 without dashes returns digit string")
    void validNoDashes_returnsDigits() {
      assertEquals("9780306406157", ISBNNormalizer.extractIsbn13("9780306406157"));
    }

    @Test
    @DisplayName("Valid ISBN-13 with dashes returns stripped digit string")
    void validWithDashes_strippedDigits() {
      assertEquals("9780306406157", ISBNNormalizer.extractIsbn13("978-0-306-40615-7"));
    }

    @Test
    @DisplayName("Non-matching string returns null")
    void noMatch_returnsNull() {
      assertNull(ISBNNormalizer.extractIsbn13("hello"));
    }

    @Test
    @DisplayName("ISBN-10 string does not match ISBN-13 pattern, returns null")
    void isbn10String_returnsNull() {
      assertNull(ISBNNormalizer.extractIsbn13("0306406152"));
    }
  }

  @Nested
  @DisplayName("extractIsbn10()")
  class ExtractIsbn10 {

    @Test
    @DisplayName("Valid ISBN-10 without dashes returns digit string")
    void validNoDashes_returnsDigits() {
      assertEquals("0306406152", ISBNNormalizer.extractIsbn10("0306406152"));
    }

    @Test
    @DisplayName("Valid ISBN-10 with dashes returns stripped digit string")
    void validWithDashes_strippedDigits() {
      assertEquals("0306406152", ISBNNormalizer.extractIsbn10("0-306-40615-2"));
    }

    @Test
    @DisplayName("Non-matching string returns null")
    void noMatch_returnsNull() {
      assertNull(ISBNNormalizer.extractIsbn10("hello"));
    }
  }

  // -------------------------------------------------------------------------
  // isbn10To13() — 5 known pairs from the Wikipedia ISBN-13 check digit table
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("isbn10To13()")
  class Isbn10To13 {

    @ParameterizedTest(name = "isbn10={0} → isbn13={1}")
    @CsvSource({
      // Source: https://en.wikipedia.org/wiki/ISBN#ISBN-13_check_digit_calculation
      // and standard ISBN conversion examples
      "0306406152, 9780306406157", // Wikipedia worked example
      "0451526538, 9780451526533", // 1984 (Orwell)
      "0743273567, 9780743273565", // The Great Gatsby
      "0316769177, 9780316769174", // The Catcher in the Rye
      "0747532699, 9780747532699", // Harry Potter and the Philosopher's Stone (UK)
    })
    @DisplayName("ISBN-10 is correctly converted to ISBN-13")
    void knownPairs(String isbn10, String expectedIsbn13) {
      assertEquals(expectedIsbn13, ISBNNormalizer.isbn10To13(isbn10));
    }
  }
}
