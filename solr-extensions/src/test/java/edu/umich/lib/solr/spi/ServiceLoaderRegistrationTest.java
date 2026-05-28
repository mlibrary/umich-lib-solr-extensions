// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.spi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umich.lib.solr.filter.AnyCallNumberNormalizerFilterFactory;
import edu.umich.lib.solr.filter.DeweyCallNumberNormalizerFilterFactory;
import edu.umich.lib.solr.filter.FullyAnchoredSearchFilterFactory;
import edu.umich.lib.solr.filter.ISBNNormalizerFilterFactory;
import edu.umich.lib.solr.filter.LCCNNormalizerFilterFactory;
import edu.umich.lib.solr.filter.LCCallNumberNormalizerFilterFactory;
import edu.umich.lib.solr.filter.LeftAnchoredSearchFilterFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.junit.jupiter.api.Test;

/**
 * Verifies that every production TokenFilterFactory is registered in the META-INF/services SPI
 * manifest and therefore discoverable by ServiceLoader.
 */
public class ServiceLoaderRegistrationTest {

  private static final Set<String> EXPECTED_NAMES =
      Set.of(
          LeftAnchoredSearchFilterFactory.NAME,
          FullyAnchoredSearchFilterFactory.NAME,
          ISBNNormalizerFilterFactory.NAME,
          LCCNNormalizerFilterFactory.NAME,
          LCCallNumberNormalizerFilterFactory.NAME,
          DeweyCallNumberNormalizerFilterFactory.NAME,
          AnyCallNumberNormalizerFilterFactory.NAME);

  @Test
  void allProductionFactoriesAreRegistered() {
    Set<String> registered =
        StreamSupport.stream(TokenFilterFactory.availableTokenFilters().spliterator(), false)
            .collect(Collectors.toSet());

    for (String name : EXPECTED_NAMES) {
      assertTrue(
          registered.contains(name), "TokenFilterFactory not found in SPI registry: " + name);
    }
  }
}
