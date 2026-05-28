// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Factory for {@link ISBNNormalizerFilter}.
 *
 * <p>Supports the {@code echoInvalidInput} parameter (boolean, default {@code false}). When {@code
 * true}, tokens that contain no recognizable ISBN are passed through unchanged rather than dropped.
 */
public class ISBNNormalizerFilterFactory extends SimpleFilterFactory {

  /** SPI name used in schema.xml and for ServiceLoader registration. */
  public static final String NAME = "isbnNormalizer";

  /** No-arg constructor required by {@link java.util.ServiceLoader}. */
  public ISBNNormalizerFilterFactory() {}

  public ISBNNormalizerFilterFactory(Map<String, String> args) {
    super(args);
  }

  /** {@inheritDoc} Creates an {@link ISBNNormalizerFilter}. */
  @Override
  public ISBNNormalizerFilter create(TokenStream in) {
    return new ISBNNormalizerFilter(in, getEchoInvalidInput(), getFilterArgs());
  }
}
