// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class DeweySimpleTest {

  @ParameterizedTest
  @CsvFileSource(
      resources = "/edu/umich/lib/normalize/callnumber/dewey_verification.tsv",
      delimiterString = "->")
  void collation_key(String original, String collation) {
    DeweySimple dewey = new DeweySimple(original);
    String key = dewey.collationKey();
    if (key == null) key = "null";
    assertEquals(collation.toString(), key);
  }
}
