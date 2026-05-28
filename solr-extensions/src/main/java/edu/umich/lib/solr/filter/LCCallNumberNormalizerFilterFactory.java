// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Factory for {@link LCCallNumberNormalizerFilter}.
 *
 * <p>Normalizes LC call number tokens to a sortable key string, suitable for sort fields and (when
 * combined with an edge-ngram filter) left-anchored prefix search.
 *
 * <p>Supported parameters:
 *
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code false}) — when {@code true}, truncated call
 *       number keys are accepted.
 *   <li>{@code echoInvalidInput} (boolean, default {@code false}) — when {@code true}, tokens that
 *       cannot be parsed as an LC call number are passed through unchanged.
 * </ul>
 *
 * <h2>Schema example</h2>
 *
 * <pre>{@code
 * <fieldType name="text_lccallnumber" class="solr.TextField">
 *   <analyzer type="index">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberNormalizer" echoInvalidInput="true"/>
 *     <filter class="solr.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 *   </analyzer>
 *   <analyzer type="query">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberNormalizer" echoInvalidInput="true"/>
 *   </analyzer>
 * </fieldType>
 * }</pre>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class LCCallNumberNormalizerFilterFactory extends SimpleFilterFactory {

  /** SPI name used in schema.xml and for ServiceLoader registration. */
  public static final String NAME = "lcCallNumberNormalizer";

  /** No-arg constructor required by {@link java.util.ServiceLoader}. */
  public LCCallNumberNormalizerFilterFactory() {}

  public LCCallNumberNormalizerFilterFactory(Map<String, String> args) {
    super(normalizeArgs(args));
  }

  /** {@inheritDoc} Creates an {@link LCCallNumberNormalizerFilter}. */
  @Override
  public LCCallNumberNormalizerFilter create(TokenStream input) {
    return new LCCallNumberNormalizerFilter(input, getEchoInvalidInput(), getFilterArgs());
  }
}
