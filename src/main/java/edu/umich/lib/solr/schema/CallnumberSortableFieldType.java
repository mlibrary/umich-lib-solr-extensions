package edu.umich.lib.solr.schema;

import edu.umich.lib.normalize.callnumber.AnyCallNumberSimple;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


public class CallnumberSortableFieldType extends CallNumberSortKeyFieldType {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Boolean has_some_key_at_all(AnyCallNumberSimple cn) {
    if (cn.hasValidKey()) return true;
    if (passThroughOnError) return true;
    if (allowTruncated && cn.hasAcceptableTruncatedKey()) return true;
    return false;
  }

  @Override
  public IndexableField createField(SchemaField field, Object value) {
    if (!field.indexed() && !field.stored()) {
      if (log.isTraceEnabled())
        log.trace("Ignoring unindexed/unstored field: {}", field);
      return null;
    }

    String val = value.toString();
    if (val == null) return null;

    AnyCallNumberSimple cn = new AnyCallNumberSimple(val);

    if (!has_some_key_at_all(cn)) return null;

    org.apache.lucene.document.FieldType newType = new org.apache.lucene.document.FieldType();
    newType.setTokenized(true);
    newType.setStored(field.stored());
    newType.setIndexOptions(IndexOptions.DOCS);

    return createField(field.getName(), val, newType);
  }
}
