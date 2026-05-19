package edu.umich.lib.solr.filter;

import edu.umich.lib.normalize.callnumber.DeweySimple;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * A Solr filter that take a Dewey Call Number (/ shelf key) and
 * turns it into something that can be sorted correctly _and_
 * can be used for left-anchored search if turned into edge-ngrams.
 */

public final class DeweyCallNumberNormalizerFilter extends TokenFilter {
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

  private Boolean passThroughOnError;
  private Boolean allowTruncated;

  /**
   * @param aStream            the upstream token stream
   * @param allowTruncated     when {@code true}, truncated call number keys are accepted
   * @param passThroughOnError when {@code true}, tokens that cannot be parsed as a
   *                           Dewey call number are passed through unchanged
   */
  public DeweyCallNumberNormalizerFilter(TokenStream aStream, Boolean allowTruncated, Boolean passThroughOnError) {
    super(aStream);
    this.allowTruncated     = allowTruncated;
    this.passThroughOnError = passThroughOnError;
  }

  public DeweyCallNumberNormalizerFilter(TokenStream aStream) {
    this(aStream, false, true);
  }


  /**
   * Normalizes the next Dewey call number token to a sortable key string.
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
        DeweySimple dewey = new DeweySimple(t);
        String key = dewey.bestKey(allowTruncated, passThroughOnError);
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
