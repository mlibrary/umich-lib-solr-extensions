package edu.umich.lib.solr.schema;

import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.StrField;
import org.apache.solr.schema.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * A {@link org.apache.solr.schema.StrField} variant that stores the raw string value
 * but indexes it via the analysis chain of a named {@code fieldType}.
 *
 * <p>Configure with a {@code fieldType} attribute pointing to a single-token field type
 * (e.g. one that applies ISBN or LCCN normalization through a {@code KeywordTokenizer}).
 * {@link #toInternal(String)} runs the input through that field type's index analyzer and
 * returns the resulting term as the stored/indexed value.
 *
 * <p>Required schema attribute: {@code fieldType} — the name of the field type whose
 * index analyzer is used for normalization.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class AnalyzedString extends StrField {

  String fieldType;
  IndexSchema schema;
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected void init(IndexSchema s, Map<String, String> args) {
    schema = s;
    super.init(schema, args);
    fieldType = args.remove("fieldType");
    if (fieldType == null) {
      throw new SolrException(
          SolrException.ErrorCode.BAD_REQUEST,
          this.getClass().toString() + " needs a fieldType attribute");
    }
  }

  @Override
  public String toInternal(String val) {
    try {
      TextField ft = (TextField) schema.getFieldTypeByName(fieldType);
      return TextField.analyzeMultiTerm(fieldType, val, ft.getIndexAnalyzer()).utf8ToString();
    } catch(SolrException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Can't create fieldType " + this.typeName + "; field " + fieldType + " doesn't exist");
    }
  }
}
