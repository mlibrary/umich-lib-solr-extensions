// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Factory for {@link DeweyCallNumberNormalizerFilter}.
 *
 * <p>Normalizes Dewey call number tokens to a sortable key string, suitable for sort fields and
 * (when combined with an edge-ngram filter) left-anchored prefix search.
 *
 * <p>Supported parameters:
 *
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code false}) — when {@code true}, truncated call
 *       number keys are accepted.
 *   <li>{@code echoInvalidInput} (boolean, default {@code false}) — when {@code true}, tokens that
 *       cannot be parsed as a Dewey call number are passed through unchanged.
 * </ul>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class DeweyCallNumberNormalizerFilterFactory extends SimpleFilterFactory {

  /** SPI name used in schema.xml and for ServiceLoader registration. */
  public static final String NAME = "deweyCallNumberNormalizer";

  /** No-arg constructor required by {@link java.util.ServiceLoader}. */
  public DeweyCallNumberNormalizerFilterFactory() {}

  public DeweyCallNumberNormalizerFilterFactory(Map<String, String> args) {
    super(normalizeArgs(args));
  }

  /** {@inheritDoc} Creates a {@link DeweyCallNumberNormalizerFilter}. */
  @Override
  public DeweyCallNumberNormalizerFilter create(TokenStream input) {
    return new DeweyCallNumberNormalizerFilter(input, getEchoInvalidInput(), getFilterArgs());
  }
}
