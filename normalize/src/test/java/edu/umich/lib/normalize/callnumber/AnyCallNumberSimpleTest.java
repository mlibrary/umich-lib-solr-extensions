// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class AnyCallNumberSimpleTest {

  @ParameterizedTest
  @CsvFileSource(
      resources = "/edu/umich/lib/normalize/callnumber/any_valid_key_verification.tsv",
      delimiterString = "->")
  void any_valid_key(String original, String collated) {
    AnyCallNumberSimple acn = new AnyCallNumberSimple(original);
    String computed = acn.anyAcceptableKey();
    if (computed == null) computed = "null";
    assertEquals(collated.toString(), computed);
  }

  @Test
  void valid_truncated_key() {
    // Letters-only LC stub — valid truncated via LC sub-object
    AnyCallNumberSimple lcTruncated = new AnyCallNumberSimple("AB");
    assertFalse(lcTruncated.hasValidKey(), "pure letters should not be a full valid key");
    assertTrue(
        lcTruncated.hasAcceptableTruncatedKey(),
        "pure letters should be an acceptable truncated key");
    assertEquals("ab", lcTruncated.acceptableTruncatedKey());

    // Fully invalid input — no truncated key available
    AnyCallNumberSimple neither = new AnyCallNumberSimple("8AB");
    assertFalse(neither.hasValidKey());
    assertFalse(neither.hasAcceptableTruncatedKey());
    assertNull(neither.acceptableTruncatedKey());
  }

  @Test
  void invalid_key() {
    // invalidKey() delegates to LCCallNumberSimple.invalidKey(), which lowercases
    // and inserts a space between a digit and a following letter
    AnyCallNumberSimple acn = new AnyCallNumberSimple("8AB");
    assertFalse(acn.hasValidKey());
    assertNotNull(acn.invalidKey());
    assertEquals("8 ab", acn.invalidKey());

    // A fully garbage string still produces a non-null invalid key
    AnyCallNumberSimple garbage = new AnyCallNumberSimple("???");
    assertNotNull(garbage.invalidKey());
  }
}
