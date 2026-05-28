// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.schema;

import edu.umich.lib.normalize.callnumber.AnyCallNumber;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.StrField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Solr {@link StrField} subtype that converts a raw call number into a sortable collation key
 * using {@link AnyCallNumber}.
 *
 * <p>The stored value combines the normalized call-number key, an end-of-call-number sentinel
 * ({@code \u001F}), the field delimiter ({@code }}), and any additional fields bundled in the
 * original value.
 *
 * <p>Optional schema attributes:
 *
 * <ul>
 *   <li>{@code allowTruncated} (default {@code true}) — whether letter- or digit-only prefixes that
 *       do not form a fully valid call number are accepted as truncated range-query endpoints.
 *   <li>{@code echoInvalidInput} (default {@code false}) — whether unrecognised call numbers are
 *       indexed as a cleaned-up freetext key instead of being silently dropped.
 * </ul>
 */
public class CallNumberSortKeyFieldType extends StrField {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected Boolean allowTruncated = true;
  protected Boolean echoInvalidInput = false;

  // Field delimiter sorts last
  private static final String FIELD_DELIMITER = "}";

  // End of callnumber sorts first (so A1<field delim> sorts before A1 1<field delim>
  private static final String END_OF_CALLNUMBER = "\u001F";

  /**
   * Initializes the field type, extracting {@code allowTruncated} and {@code echoInvalidInput}
   * options from {@code args}.
   *
   * @param schema the current {@link org.apache.solr.schema.IndexSchema}
   * @param args the field-type configuration arguments from {@code schema.xml}
   */
  protected void init(IndexSchema schema, Map<String, String> args) {
    super.init(schema, args);

    String trunc = args.remove("allowTruncated");
    if (trunc != null) {
      allowTruncated = Boolean.parseBoolean(trunc);
    }
    String ptoe = args.remove("echoInvalidInput");
    if (ptoe != null) {
      echoInvalidInput = Boolean.parseBoolean(ptoe);
    }
  }

  /**
   * Normalizes a call-number value (optionally with appended bundled fields) to its sortable form.
   *
   * <p>The input may contain a {@code }} delimiter; everything before the first delimiter is
   * treated as the call number and everything after as appended fields that are re-bundled into the
   * output unchanged.
   *
   * @param val the raw value from the document, optionally containing {@code }} followed by
   *     additional fields
   * @return the sortable, bundled value; or {@code null} when the call number is unrecognised and
   *     {@code echoInvalidInput} is {@code false}
   */
  @Override
  public String toInternal(String val) {
    String[] fields = val.split(FIELD_DELIMITER, 2);
    String appendedFields = "";
    if (fields.length > 1) {
      appendedFields = fields[1];
    }

    AnyCallNumber cn = new AnyCallNumber(fields[0]);

    // Valid? Return it
    if (cn.hasValidKey()) {
      return bundledFields(cn.validKey(), appendedFields);
    }
    if (allowTruncated && cn.hasAcceptableTruncatedKey()) {
      return bundledFields(cn.acceptableTruncatedKey(), appendedFields);
    }

    // Not valid at all, so if we're not passing through, return null.
    if (echoInvalidInput) {
      return bundledFields(cn.invalidKey(), appendedFields);
    } else {
      return null;
    }
  }

  /**
   * Assembles the final stored value as {@code normalizedCn + END_OF_CALLNUMBER + FIELD_DELIMITER +
   * appendedField}.
   *
   * <p>{@code END_OF_CALLNUMBER} ({@code \u001F}) sorts before any printable character so that a
   * bare call number sorts before the same call number with sub-fields appended. {@code
   * FIELD_DELIMITER} ({@code }}) sorts after letters and digits so that call-number segments sort
   * correctly.
   *
   * @param normalizedCn the collation key produced by the call-number parser
   * @param appendedField any additional fields that were bundled in the raw value
   * @return the complete, delimited sort key
   */
  public String bundledFields(String normalizedCn, String appendedField) {
    return normalizedCn + END_OF_CALLNUMBER + FIELD_DELIMITER + appendedField;
  }
}
