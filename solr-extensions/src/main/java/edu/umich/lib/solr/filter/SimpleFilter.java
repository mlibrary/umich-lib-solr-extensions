// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Abstract base class for simple one-token-in / one-token-out filters. Subclasses must implement
 * {@link #munge(String)}, which is the sole extension point.
 *
 * <p>If {@code munge} returns {@code null} the token is treated as invalid: it is dropped by
 * default, or echoed unchanged when {@code echoInvalidInput} is {@code true}.
 *
 * <p>Empty upstream tokens (tokens where {@code toString()} returns an empty string) are emitted
 * unchanged without calling {@code munge}. This is intentional: empty tokens carry no content to
 * normalize, and passing them through preserves downstream position accounting. A future change to
 * drop empties would be a breaking change; document it here if the behavior ever changes.
 *
 * <h2>Passing schema.xml parameters to {@code munge}</h2>
 *
 * <p>When instantiated via {@link SimpleFilterFactory}, all attributes declared in {@code
 * schema.xml} are available through {@link #getArg(String, String)}. Subclasses should read
 * parameters in their constructor:
 *
 * <pre>{@code
 * public class MyFilter extends SimpleFilter {
 *     private final String myParam;
 *
 *     public MyFilter(TokenStream in, boolean echoInvalidInput, Map<String,String> args) {
 *         super(in, echoInvalidInput, args);
 *         this.myParam = getArg("myParam", "defaultValue");
 *     }
 *
 *     @Override
 *     public String munge(String input) { ... }
 * }
 * }</pre>
 *
 * <p>Migrated from edu-umich-lib-solrMegapack pluginScaffold.
 */
public abstract class SimpleFilter extends TokenFilter {

  private final CharTermAttribute myTermAttribute = addAttribute(CharTermAttribute.class);

  private final boolean echoInvalidInput;

  /** Immutable snapshot of all schema.xml args passed from the factory. */
  private final Map<String, String> filterArgs;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  /**
   * Full constructor used by {@link SimpleFilterFactory} subclasses.
   *
   * @param in the upstream token stream
   * @param echoInvalidInput when {@code true}, tokens for which {@link #munge} returns {@code null}
   *     are passed through unchanged
   * @param args all schema.xml attributes; available via {@link #getArg(String, String)}
   */
  public SimpleFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
    super(in);
    this.echoInvalidInput = echoInvalidInput;
    this.filterArgs = Collections.unmodifiableMap(new HashMap<>(args));
  }

  // -------------------------------------------------------------------------
  // Arg access for subclasses
  // -------------------------------------------------------------------------

  /**
   * Returns the value of an optional schema.xml attribute, or the supplied default when the
   * attribute is absent. Subclasses must always supply a meaningful default — this method never
   * returns {@code null}.
   *
   * @param key the attribute name
   * @param defaultValue the value to use when the attribute is absent
   * @return the attribute value, or {@code defaultValue}
   */
  protected String getArg(String key, String defaultValue) {
    return filterArgs.getOrDefault(key, defaultValue);
  }

  // -------------------------------------------------------------------------
  // Extension point
  // -------------------------------------------------------------------------

  /**
   * Transform the input token string. Return {@code null} to signal that the token is invalid (it
   * will be dropped or echoed depending on {@code echoInvalidInput}).
   *
   * <p>This method is never called for empty tokens; those are emitted unchanged.
   *
   * @param input the non-empty token string to transform
   * @return the transformed string, or {@code null} if the token is invalid
   */
  public abstract String munge(String input);

  // -------------------------------------------------------------------------
  // Core filter logic
  // -------------------------------------------------------------------------

  /**
   * Advances to the next output token by consuming upstream tokens and running each through {@link
   * #munge(String)}.
   *
   * <p>Empty upstream tokens (tokens where {@code toString()} is empty) are emitted unchanged
   * without invoking {@code munge}.
   *
   * <p>Tokens for which {@code munge} returns {@code null} are either passed through unchanged
   * (when {@code echoInvalidInput} is {@code true}) or silently dropped (when it is {@code false}).
   *
   * @return {@code true} if an output token was produced; {@code false} when the upstream stream is
   *     exhausted
   * @throws IOException if the upstream token stream throws
   */
  @Override
  public final boolean incrementToken() throws IOException {
    while (input.incrementToken()) {
      String t = myTermAttribute.toString();
      if (t.isEmpty()) {
        return true;
      }

      myTermAttribute.setEmpty();
      String newStr = munge(t);
      if (newStr == null) {
        if (echoInvalidInput) {
          myTermAttribute.append(t);
          return true;
        }
        // else: drop this token and try the next one
      } else {
        myTermAttribute.append(newStr);
        return true;
      }
    }
    return false;
  }
}
