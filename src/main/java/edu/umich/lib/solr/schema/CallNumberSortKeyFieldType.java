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
  protected Boolean echoInvalidInput = false;

  // Field delimiter sorts last
  private static final String FIELD_DELIMITER = "}";

  // End of callnumber sorts first (so A1<field delim> sorts before A1 1<field delim>
  private static final String END_OF_CALLNUMBER = "\u001F";

  protected void init(IndexSchema schema, Map<String, String> args) {
    super.init(schema, args);

    String trunc = args.remove("allowTruncated");
    if (trunc != null) {
      allowTruncated = true;
    }
    String ptoe = args.remove("echoInvalidInput");
    if (ptoe != null) {
      echoInvalidInput = Boolean.parseBoolean(ptoe);
    }

  }


  @Override
  public String toInternal(String val) {
    String[] fields = val.split(FIELD_DELIMITER, 2);
    String appendedFields = "";
    if (fields.length > 1) {
      appendedFields = fields[1];
    }

    AnyCallNumberSimple cn = new AnyCallNumberSimple(fields[0]);

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


  public String bundledFields(String normalizedCn, String appendedField) {
    return normalizedCn + END_OF_CALLNUMBER + FIELD_DELIMITER + appendedField;
  }


}
