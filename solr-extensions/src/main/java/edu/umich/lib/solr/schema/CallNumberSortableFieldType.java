// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.schema;

import edu.umich.lib.normalize.callnumber.AnyCallNumberSimple;
import java.lang.invoke.MethodHandles;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link CallNumberSortKeyFieldType} that creates a field storing the <em>original
 * input text</em> (suitable for display) while indexing with DOCS-only options to support existence
 * queries. Sort order is still provided by the inherited {@code toInternal()} normalization.
 *
 * <p>Returns {@code null} (skipping the field) when the call number cannot produce any usable key
 * and {@code echoInvalidInput} is {@code false}.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class CallNumberSortableFieldType extends CallNumberSortKeyFieldType {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean hasSomeKeyAtAll(AnyCallNumberSimple cn) {
    if (cn.hasValidKey()) return true;
    if (echoInvalidInput) return true;
    if (allowTruncated && cn.hasAcceptableTruncatedKey()) return true;
    return false;
  }

  /**
   * Creates a Lucene field that stores the original call-number text and indexes it with {@link
   * IndexOptions#DOCS}-only options (no term frequencies or positions).
   *
   * <p>Returns {@code null} when the field is neither indexed nor stored, or when the call number
   * has no usable key in any format.
   *
   * @param field the Solr schema field definition
   * @param value the document value to index; {@link Object#toString()} is called to obtain the
   *     string form
   * @return a new {@link IndexableField}, or {@code null} if the value should not be indexed
   */
  @Override
  public IndexableField createField(SchemaField field, Object value) {
    if (!field.indexed() && !field.stored()) {
      if (log.isTraceEnabled()) log.trace("Ignoring unindexed/unstored field: {}", field);
      return null;
    }

    String val = value.toString();
    if (val == null) return null;

    AnyCallNumberSimple cn = new AnyCallNumberSimple(val);

    if (!hasSomeKeyAtAll(cn)) return null;

    org.apache.lucene.document.FieldType newType = new org.apache.lucene.document.FieldType();
    newType.setTokenized(true);
    newType.setStored(field.stored());
    newType.setIndexOptions(IndexOptions.DOCS);

    return createField(field.getName(), val, newType);
  }
}
