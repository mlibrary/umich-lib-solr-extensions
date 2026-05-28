// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Solr/Lucene token filter that uppercases any token whose first character matches a configured
 * letter (case-insensitively).
 *
 * <p>Tokens that do <em>not</em> start with the configured letter are passed through unchanged;
 * this filter never drops tokens.
 *
 * <h2>Parameters</h2>
 *
 * <dl>
 *   <dt>{@code letter} (required)
 *   <dd>A single alphabetic character. Tokens whose first character matches this letter
 *       (case-insensitively) will be uppercased. Must be a letter; non-letter values and values of
 *       length other than 1 are rejected at construction time.
 *   <dt>{@code reverse} (optional, default {@code false})
 *   <dd>When {@code true}, the uppercased result is also reversed character-by-character before
 *       being emitted.
 * </dl>
 *
 * <h2>Usage example (schema.xml)</h2>
 *
 * <pre>{@code
 * <analyzer>
 *   <tokenizer class="solr.WhitespaceTokenizerFactory"/>
 *   <filter class="solr.UpcaseWordsThatStartWithFilterFactory"
 *           letter="a" reverse="false"/>
 * </analyzer>
 * }</pre>
 */
public final class UpcaseWordsThatStartWithFilter extends SimpleFilter {

  private final char letter;
  private final boolean reverse;

  /**
   * Constructs the filter.
   *
   * @param in the upstream token stream
   * @param echoInvalidInput forwarded to {@link SimpleFilter}; effectively unused because {@link
   *     #munge} never returns {@code null}
   * @param args schema.xml attributes forwarded from the factory; must contain {@code letter}
   * @throws IllegalArgumentException if {@code letter} is absent, not exactly one character, or not
   *     an alphabetic letter
   */
  UpcaseWordsThatStartWithFilter(
      TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
    super(in, echoInvalidInput, args);
    String raw = getArg("letter", null);
    if (raw == null) {
      throw new IllegalArgumentException(
          "UpcaseWordsThatStartWithFilter requires a 'letter' argument");
    }
    if (raw.length() != 1) {
      throw new IllegalArgumentException(
          "UpcaseWordsThatStartWithFilter 'letter' must be exactly one character; got: \""
              + raw
              + "\"");
    }
    if (!Character.isLetter(raw.charAt(0))) {
      throw new IllegalArgumentException(
          "UpcaseWordsThatStartWithFilter 'letter' must be an alphabetic letter; got: \""
              + raw
              + "\"");
    }
    this.letter = Character.toLowerCase(raw.charAt(0));
    this.reverse = Boolean.parseBoolean(getArg("reverse", "false"));
  }

  /**
   * If {@code input} starts with the configured letter (case-insensitively), returns the uppercased
   * token (and optionally reverses it). Otherwise returns {@code input} unchanged.
   *
   * @param input the token string to process
   * @return the transformed token; never {@code null}
   */
  @Override
  public String munge(String input) {
    if (input.isEmpty()) {
      return input;
    }
    if (Character.toLowerCase(input.charAt(0)) == letter) {
      String result = input.toUpperCase();
      return reverse ? new StringBuilder(result).reverse().toString() : result;
    }
    return input;
  }
}
