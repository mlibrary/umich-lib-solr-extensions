package edu.umich.lib.solr.testing;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Legacy token-stream test helpers.
 *
 * @deprecated Use {@link TokenStreamAsserter} instead. This class predates
 *             {@code TokenStreamAsserter} and uses raw arrays and a less
 *             structured API. It will be removed in a future release.
 */
@Deprecated
public class TokenStreamTestHelpers {

    /*
  helper method to get simpletokens for any given TokenStream
   */

  private static final String[] EMPTY_ARRAY = {};

  public static ArrayList<ManualTokenStream.SimpleToken> getSimpleTokens(TokenStream ts) throws IOException {
    CharTermAttribute ta = ts.addAttribute(CharTermAttribute.class);
    PositionIncrementAttribute pia = ts.addAttribute(PositionIncrementAttribute.class);

    ArrayList<ManualTokenStream.SimpleToken> sts = new ArrayList<>();
    int currentTokenPosition = 0;
    while (ts.incrementToken()) {
      currentTokenPosition += pia.getPositionIncrement();
      sts.add(new ManualTokenStream.SimpleToken(ta.toString(), currentTokenPosition));
    }
    return sts;
  }

  public static String[] getTerms(TokenStream ts) throws IOException {
    return getSimpleTokens(ts).stream().map(s -> s.text).toArray(String[]::new);
  }

  public static List<String[]> getNestedTerms(TokenStream ts) throws IOException {
    ArrayList<ManualTokenStream.SimpleToken> tokens = getSimpleTokens(ts);
    int lastPosition = tokens.get(tokens.size() - 1).position;
    ArrayList<ArrayList<String>> values = new ArrayList<ArrayList<String>>(lastPosition);
    for(int i = 0; i < lastPosition; i++) {
      values.add(i, new ArrayList<String>());
    }
    tokens.forEach(st ->{
      int arrayPos = st.position - 1;
      ArrayList<String> positionalValues = values.get(arrayPos);
      positionalValues.add(st.text);
    } );
    return values.stream().map(al -> al.toArray(EMPTY_ARRAY)).collect(Collectors.toList());
  }
}
