// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class LCCallNumberTest {

  @ParameterizedTest
  @CsvFileSource(
      resources = "/edu/umich/lib/normalize/callnumber/lc__verification.tsv",
      delimiterString = "->")
  void collation_key(String original, String collation) {
    LCCallNumber lccs = new LCCallNumber(original);
    String key = lccs.collationKey();
    if (key == null) key = "null";
    assertEquals(collation.trim(), key);
  }
}
