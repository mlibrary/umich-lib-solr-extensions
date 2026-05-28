// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.normalize.callnumber;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A call number that accepts either LC or Dewey format, picking the first valid interpretation. If
 * neither format produces a valid parse, the input is treated as an unrecognised string and
 * returned via {@link #invalidKey()}.
 */
public class AnyCallNumberSimple extends AbstractCallNumber {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public String eitherValidKey;
  public LCCallNumberSimple lc;
  public DeweySimple dewey;

  /**
   * Parses {@code str} as both an LC and a Dewey call number and selects whichever format produces
   * a valid key (LC takes precedence).
   *
   * @param str the raw call-number string to parse
   */
  public AnyCallNumberSimple(String str) {
    trimmedOriginal = str;
    lc = new LCCallNumberSimple(str);
    dewey = new DeweySimple(str);
    eitherValidKey = chooseLCOrDeweyValidKey(trimmedOriginal);
    isValid = !(eitherValidKey == null);
  }

  /**
   * @return {@code true} if either the LC or Dewey parse produced a valid key
   */
  @Override
  public Boolean hasValidKey() {
    return isValid;
  }

  /**
   * @return the collation key of whichever format was valid, or {@code null}
   */
  @Override
  public String validKey() {
    return eitherValidKey;
  }

  /**
   * @return {@code true} if either the LC or Dewey parse produced an acceptable truncated key
   */
  @Override
  public Boolean hasAcceptableTruncatedKey() {
    return (lc.hasAcceptableTruncatedKey() || dewey.hasAcceptableTruncatedKey());
  }

  /**
   * Returns the truncated key preferred in LC order: LC key first, then Dewey, then {@code null} if
   * neither applies.
   */
  @Override
  public String acceptableTruncatedKey() {
    if (lc.hasAcceptableTruncatedKey()) return lc.acceptableTruncatedKey();
    if (dewey.hasAcceptableTruncatedKey()) return dewey.acceptableTruncatedKey();
    return null;
  }

  /**
   * Returns a freetext fallback key for unrecognised call numbers by delegating to the LC parser's
   * {@code invalidKey()} implementation.
   */
  @Override
  public String invalidKey() {
    return lc.invalidKey();
  }

  private String chooseLCOrDeweyValidKey(String str) {
    if (lc.isValid) return lc.collationKey();
    if (dewey.isValid) return dewey.collationKey();
    return null;
  }
}
