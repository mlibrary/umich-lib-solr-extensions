// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Map;
import org.apache.lucene.analysis.TokenStream;

/**
 * Factory for {@link LCCNNormalizerFilter}.
 *
 * <p>Supports the {@code echoInvalidInput} parameter (boolean, default {@code false}), though in
 * practice {@link LCCNNormalizerFilter#munge(String)} never returns {@code null} so the parameter
 * has no effect.
 */
public class LCCNNormalizerFilterFactory extends SimpleFilterFactory {

  /** SPI name used in schema.xml and for ServiceLoader registration. */
  public static final String NAME = "lccnNormalizer";

  /** No-arg constructor required by {@link java.util.ServiceLoader}. */
  public LCCNNormalizerFilterFactory() {}

  /** Creates a factory pre-configured with {@code args}. */
  public LCCNNormalizerFilterFactory(Map<String, String> args) {
    super(args);
  }

  /** {@inheritDoc} Creates an {@link LCCNNormalizerFilter}. */
  @Override
  public LCCNNormalizerFilter create(TokenStream in) {
    return new LCCNNormalizerFilter(in, getEchoInvalidInput(), getFilterArgs());
  }
}
