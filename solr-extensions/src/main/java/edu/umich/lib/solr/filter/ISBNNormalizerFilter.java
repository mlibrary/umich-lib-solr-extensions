// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import edu.umich.lib.normalize.isbn.ISBNNormalizer;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Solr/Lucene token filter that normalizes ISBN strings to ISBN-13.
 *
 * <p>Tokens that contain no recognizable ISBN pattern are dropped (or passed through unchanged if
 * {@code echoInvalidInput="true"} is set in schema.xml). Diagnostic logging is handled by {@link
 * ISBNNormalizer}.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class ISBNNormalizerFilter extends SimpleFilter {

  /**
   * Factory-only constructor. Instantiate via {@link ISBNNormalizerFilterFactory}.
   *
   * @param in the upstream token stream
   * @param echoInvalidInput when {@code true}, tokens that are not valid ISBNs are passed through
   *     unchanged rather than dropped
   * @param args schema.xml attributes forwarded from the factory
   */
  ISBNNormalizerFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
    super(in, echoInvalidInput, args);
  }

  /**
   * Normalizes {@code input} to an ISBN-13 string.
   *
   * @return the normalized ISBN-13, or {@code null} if {@code input} contains no recognizable ISBN
   */
  @Override
  public String munge(String input) {
    return ISBNNormalizer.normalize(input);
  }
}
