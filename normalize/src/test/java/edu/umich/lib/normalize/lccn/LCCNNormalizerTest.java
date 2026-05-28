// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.lccn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Normalization cases taken from the Business::LCCN perl module test suite. See:
 * library_stdnums/spec/library_stdnums_spec.rb
 */
@DisplayName("LCCNNormalizer — perl module test suite")
class LCCNNormalizerTest {

  static Stream<Arguments> normalizeCases() {
    return Stream.of(
        // Two-part (pre-2001): alpha prefix + 2-digit year + serial
        Arguments.of("n78-890351", "n78890351"),
        Arguments.of("n 78890351 ", "n78890351"),

        // Two-part (pre-2001): no alpha prefix
        Arguments.of(" 85000002 ", "85000002"),
        Arguments.of("85-2 ", "85000002"),
        Arguments.of("2001-000002", "2001000002"),

        // Revision suffixes (//r, /AC/r) — everything after first slash stripped
        Arguments.of("75-425165//r75", "75425165"),
        Arguments.of(" 79139101 /AC/r932", "79139101"),

        // Short serial zero-padding
        Arguments.of("89-4", "89000004"),
        Arguments.of("89-45", "89000045"),
        Arguments.of("89-456", "89000456"),
        Arguments.of("89-1234", "89001234"),
        Arguments.of("89-001234", "89001234"),
        Arguments.of("89001234", "89001234"),

        // Four-digit year (post-2000), no prefix
        Arguments.of("2002-1234", "2002001234"),
        Arguments.of("2002-001234", "2002001234"),
        Arguments.of("2002001234", "2002001234"),

        // Leading/trailing whitespace
        Arguments.of("   89001234 ", "89001234"),
        Arguments.of("  2002001234", "2002001234"),

        // One-letter prefix, pre-2001
        Arguments.of("a89-1234", "a89001234"),
        Arguments.of("a89-001234", "a89001234"),
        Arguments.of("a89001234", "a89001234"),

        // One-letter prefix, post-2000
        Arguments.of("a2002-1234", "a2002001234"),
        Arguments.of("a2002-001234", "a2002001234"),
        Arguments.of("a2002001234", "a2002001234"),

        // One-letter prefix with interior spaces
        Arguments.of("a 89001234 ", "a89001234"),
        Arguments.of("a 89-001234 ", "a89001234"),
        Arguments.of("a 2002001234", "a2002001234"),

        // Two-letter prefix, pre-2001
        Arguments.of("ab89-1234", "ab89001234"),
        Arguments.of("ab89-001234", "ab89001234"),
        Arguments.of("ab89001234", "ab89001234"),

        // Two-letter prefix, post-2000
        Arguments.of("ab2002-1234", "ab2002001234"),
        Arguments.of("ab2002-001234", "ab2002001234"),
        Arguments.of("ab2002001234", "ab2002001234"),

        // Two-letter prefix with interior spaces
        Arguments.of("ab 89001234 ", "ab89001234"),
        Arguments.of("ab 2002001234", "ab2002001234"),
        Arguments.of("ab 89-1234", "ab89001234"),

        // Three-letter prefix
        Arguments.of("abc89-1234", "abc89001234"),
        Arguments.of("abc89-001234", "abc89001234"),
        Arguments.of("abc89001234", "abc89001234"),
        Arguments.of("abc89001234 ", "abc89001234"),

        // lccn.loc.gov URLs — path component after last slash is the LCCN
        Arguments.of("http://lccn.loc.gov/89001234", "89001234"),
        Arguments.of("http://lccn.loc.gov/a89001234", "a89001234"),
        Arguments.of("http://lccn.loc.gov/ab89001234", "ab89001234"),
        Arguments.of("http://lccn.loc.gov/abc89001234", "abc89001234"),
        Arguments.of("http://lccn.loc.gov/2002001234", "2002001234"),
        Arguments.of("http://lccn.loc.gov/a2002001234", "a2002001234"),
        Arguments.of("http://lccn.loc.gov/ab2002001234", "ab2002001234"),

        // Miscellaneous real-world forms
        Arguments.of("00-21595", "00021595"),
        Arguments.of("2001001599", "2001001599"),
        Arguments.of("99-18233", "99018233"),
        Arguments.of("98000595", "98000595"),
        Arguments.of("99005074", "99005074"),
        Arguments.of("00003373", "00003373"),
        Arguments.of("01001599", "01001599"),
        Arguments.of("   95156543 ", "95156543"),

        // Revision suffixes with /AC
        Arguments.of("   94014580 /AC/r95", "94014580"),
        Arguments.of("   79310919 //r86", "79310919"),

        // Two-letter prefix with trailing spaces
        Arguments.of("gm 71005810  ", "gm71005810"),
        Arguments.of("sn2006058112  ", "sn2006058112"),
        Arguments.of("gm 71-2450", "gm71002450"),

        // Four-digit year, post-2000, short serial
        Arguments.of("2001-1114", "2001001114"));
  }

  @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
  @MethodSource("normalizeCases")
  @DisplayName("normalize")
  void normalize(String input, String expected) {
    assertEquals(expected, LCCNNormalizer.normalize(input));
  }
}
