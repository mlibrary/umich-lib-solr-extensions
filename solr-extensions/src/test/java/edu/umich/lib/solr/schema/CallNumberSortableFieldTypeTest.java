// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.umich.lib.normalize.callnumber.AnyCallNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CallNumberSortableFieldType}.
 *
 * <p>{@code CallNumberSortableFieldType} overrides {@code createField()} to store the
 * <em>original</em> display value while inheriting sort-key normalisation from {@link
 * CallNumberSortKeyFieldType}.
 *
 * <p>The {@code createField()} override requires a live {@code SchemaField} (which in turn needs an
 * {@code IndexSchema}); that path is covered by {@link
 * edu.umich.lib.solr.integration.live.CallNumberSortLiveIT}. These unit tests focus on the
 * behaviours that are exercisable without a Solr container:
 *
 * <ul>
 *   <li>Inheritance: {@code toInternal()} works identically to the parent.
 *   <li>Gate logic: the {@code hasSomeKeyAtAll} predicate (package-visible via subclass reflection)
 *       drives whether {@code createField} returns {@code null}; we verify the underlying {@link
 *       AnyCallNumber} state that drives those decisions.
 *   <li>Configuration: {@code allowTruncated} and {@code echoInvalidInput} behave as documented.
 * </ul>
 */
@DisplayName("CallNumberSortableFieldType")
class CallNumberSortableFieldTypeTest {

  private CallNumberSortableFieldType fieldType;

  @BeforeEach
  void setUp() {
    fieldType = new CallNumberSortableFieldType();
    // Default config: allowTruncated=true, echoInvalidInput=false
  }

  // -----------------------------------------------------------------------
  // Inheritance from CallNumberSortKeyFieldType
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("inherits toInternal from CallNumberSortKeyFieldType")
  class InheritedToInternal {

    @Test
    @DisplayName("is a subtype of CallNumberSortKeyFieldType")
    void isSubtype() {
      assertInstanceOf(CallNumberSortKeyFieldType.class, fieldType);
    }

    @Test
    @DisplayName("valid LC call number returns the same key as the parent type")
    void validLCSameAsParent() {
      CallNumberSortKeyFieldType parent = new CallNumberSortKeyFieldType();
      String expected = parent.toInternal("PS3537.A832 B6 1948");
      assertEquals(expected, fieldType.toInternal("PS3537.A832 B6 1948"));
    }

    @Test
    @DisplayName("valid Dewey call number returns the same key as the parent type")
    void validDeweySameAsParent() {
      CallNumberSortKeyFieldType parent = new CallNumberSortKeyFieldType();
      String expected = parent.toInternal("813.54 SAL");
      assertEquals(expected, fieldType.toInternal("813.54 SAL"));
    }

    @Test
    @DisplayName("sort order is preserved: A1 < PS3537 < QA76 < ZZ999")
    void sortOrderPreserved() {
      String a = fieldType.toInternal("A1 .B2 1900");
      String b = fieldType.toInternal("PS3537.A832 B6 1948");
      String c = fieldType.toInternal("QA76.73.J38 G67 2018");
      String d = fieldType.toInternal("ZZ999 .Z99 9999");
      assertNotNull(a);
      assertNotNull(b);
      assertNotNull(c);
      assertNotNull(d);
      assertTrue(a.compareTo(b) < 0);
      assertTrue(b.compareTo(c) < 0);
      assertTrue(c.compareTo(d) < 0);
    }
  }

  // -----------------------------------------------------------------------
  // hasSomeKeyAtAll gate — tested via AnyCallNumber state
  //
  // createField() skips (returns null) when hasSomeKeyAtAll() is false.
  // That condition holds when:
  //   !hasValidKey && !echoInvalidInput && !(allowTruncated && hasAcceptableTruncatedKey)
  //
  // We verify the underlying AnyCallNumber state so we can be confident about
  // which inputs will be dropped by createField() in the live IT.
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("hasSomeKeyAtAll gate — AnyCallNumber state")
  class GateLogic {

    @Test
    @DisplayName("valid LC input: hasValidKey=true → field will be created")
    void validLCHasKey() {
      AnyCallNumber cn = new AnyCallNumber("PS3537.A832 B6 1948");
      assertTrue(cn.hasValidKey(), "valid LC should have a key");
    }

    @Test
    @DisplayName("valid Dewey input: hasValidKey=true → field will be created")
    void validDeweyHasKey() {
      AnyCallNumber cn = new AnyCallNumber("813.54 SAL");
      assertTrue(cn.hasValidKey(), "valid Dewey should have a key");
    }

    @Test
    @DisplayName(
        "truncated LC (letters only): hasAcceptableTruncatedKey=true → field will be created when allowTruncated=true")
    void truncatedLCHasTruncatedKey() {
      AnyCallNumber cn = new AnyCallNumber("PS");
      assertFalse(cn.hasValidKey(), "letters-only should not be a full valid key");
      assertTrue(
          cn.hasAcceptableTruncatedKey(),
          "letters-only LC stub should be an acceptable truncated key");
    }

    @Test
    @DisplayName(
        "fully invalid input: no valid or truncated key → field will be skipped (echoInvalidInput=false)")
    void invalidInputSkipped() {
      AnyCallNumber cn = new AnyCallNumber("!@#$%");
      assertFalse(cn.hasValidKey());
      assertFalse(cn.hasAcceptableTruncatedKey());
      // toInternal returns null for this input with default config
      assertNull(
          fieldType.toInternal("!@#$%"),
          "invalid input with echoInvalidInput=false should produce null sort key");
    }

    @Test
    @DisplayName(
        "echoInvalidInput=true: invalid input still produces a sort key → field will be created")
    void echoInvalidInputCreatesKey() {
      fieldType.echoInvalidInput = true;
      assertNotNull(
          fieldType.toInternal("!@#$%"),
          "echoInvalidInput=true should produce a non-null key for garbage input");
    }

    @Test
    @DisplayName("allowTruncated=false: truncated-only input produces null → field will be skipped")
    void noTruncationSkipsField() {
      fieldType.allowTruncated = false;
      assertNull(
          fieldType.toInternal("PS"), "allowTruncated=false should reject truncated-only LC stub");
    }
  }
}
