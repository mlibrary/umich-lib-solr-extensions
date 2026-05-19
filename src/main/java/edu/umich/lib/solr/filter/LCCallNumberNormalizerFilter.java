package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umich.lib.normalize.callnumber.LCCallNumberSimple;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * A Solr/Lucene token filter that normalizes LC Call Number tokens to a
 * sortable key string suitable for both sort fields and left-anchored prefix search
 * (when paired with an edge-ngram filter).
 */

public final class LCCallNumberNormalizerFilter extends TokenFilter {
  /**
   * Logger used to log info/warnings.
   */
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The filter term that is a result of the conversion.
   */
  private final CharTermAttribute myTermAttribute =
      addAttribute(CharTermAttribute.class);

  /**
   * Should we pass through an invalid (doesn't look like) callnumber,
   * or return nothing (default)
   */

  private Boolean allowTruncated;
  private Boolean passThroughOnError;


  /**
   * @param aStream            the upstream token stream
   * @param allowTruncated     when {@code true}, truncated call number keys are accepted
   * @param passThroughOnError when {@code true}, tokens that cannot be parsed as an
   *                           LC call number are passed through unchanged
   */
  public LCCallNumberNormalizerFilter(TokenStream aStream,  Boolean allowTruncated, Boolean passThroughOnError) {
    super(aStream);
    this.allowTruncated     = allowTruncated;
    this.passThroughOnError = passThroughOnError;
  }

  public LCCallNumberNormalizerFilter(TokenStream aStream) {
    this(aStream, false, true);
  }


  /**
   * Normalizes the next LC call number token to a sortable key string.
   *
   * @return {@code true} if a token was produced; {@code false} at end of stream
   */
  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) {
      return false;
    }

    String t = myTermAttribute.toString();
    if (t != null && t.length() != 0) {
      try {
        myTermAttribute.setEmpty();
        LCCallNumberSimple lc = new LCCallNumberSimple(t);
        String key = lc.bestKey(allowTruncated, passThroughOnError);
        if (key == null) {
          return false;
        } else {
          myTermAttribute.append(key);
        }
      } catch (IllegalArgumentException details) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(details.getMessage(), details);
        }
      }
    }

    return true;
  }
}
