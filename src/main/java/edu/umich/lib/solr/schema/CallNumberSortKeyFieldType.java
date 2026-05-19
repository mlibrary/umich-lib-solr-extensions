package edu.umich.lib.solr.schema;

import edu.umich.lib.normalize.callnumber.AnyCallNumberSimple;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.StrField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

public class CallNumberSortKeyFieldType extends StrField {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected Boolean allowTruncated = true;
  protected Boolean passThroughOnError = false;

  // Field delimiter sorts last
  private final String FIELD_DELIMITER = "}";

  // End of callnumber sorts first (so A1<field delim> sorts before A1 1<field delim>
  private final String END_OF_CALLNUMBER = "\u001F";

  protected void init(IndexSchema schema, Map<String, String> args) {
    super.init(schema, args);

    String trunc = args.remove("allowTruncated");
    if (trunc != null) {
      allowTruncated = true;
    }
    String ptoe = args.remove("passThroughOnError");
    if (ptoe != null) {
      passThroughOnError = Boolean.parseBoolean(ptoe);
    }

  }


  @Override
  public String toInternal(String val) {
    String[] fields = val.split(FIELD_DELIMITER, 2);
    String appended_fields = "";
    if (fields.length > 1) {
      appended_fields = fields[1];
    }

    AnyCallNumberSimple cn = new AnyCallNumberSimple(fields[0]);

    // Valid? Return it
    if (cn.hasValidKey()) {
      return bundled_fields(cn.validKey(), appended_fields);
    }
    if (allowTruncated && cn.hasAcceptableTruncatedKey()) {
      return bundled_fields(cn.acceptableTruncatedKey(), appended_fields);
    }

    // Not valid at all, so if we're not passing through, return null.
    if (passThroughOnError) {
      return bundled_fields(cn.invalidKey(), appended_fields);
    } else {
      return null;
    }
  }


  public String bundled_fields(String normalized_cn, String appended_field) {
    return normalized_cn + END_OF_CALLNUMBER + FIELD_DELIMITER + appended_field;
  }


}
