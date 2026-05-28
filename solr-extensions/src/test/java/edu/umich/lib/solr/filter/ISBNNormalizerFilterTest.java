// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import static edu.umich.lib.solr.testing.TokenStreamAsserter.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.junit.jupiter.api.*;

/**
 * Integration tests for {@link ISBNNormalizerFilter} and {@link ISBNNormalizerFilterFactory}.
 *
 * <p>Tests use a {@link WhitespaceTokenizer} source so each whitespace-delimited token flows
 * through {@link ISBNNormalizerFilter#munge(String)} independently.
 */
@DisplayName("ISBNNormalizerFilter")
class ISBNNormalizerFilterTest {

  // Shared analyzer: WhitespaceTokenizer → ISBNNormalizerFilter (echoInvalidInput=false)
  private Analyzer analyzer;

  @BeforeEach
  void setUp() {
    var factory = new ISBNNormalizerFilterFactory(Map.of());
    analyzer =
        new Analyzer() {
          @Override
          protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer source = new WhitespaceTokenizer();
            TokenStream result = factory.create(source);
            return new TokenStreamComponents(source, result);
          }
        };
  }

  @AfterEach
  void tearDown() {
    analyzer.close();
  }

  // -------------------------------------------------------------------------
  // Token normalization
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Token normalization")
  class TokenNormalization {

    @Test
    @DisplayName("Valid ISBN-13 token is emitted normalized (dashes stripped)")
    void isbn13WithDashes_emittedNormalized() throws IOException {
      var tokens = analyze(analyzer, "978-0-306-40615-7");
      assertTerms(tokens, "9780306406157");
    }

    @Test
    @DisplayName("Valid ISBN-10 token is converted to ISBN-13")
    void isbn10_convertedToIsbn13() throws IOException {
      var tokens = analyze(analyzer, "0306406152");
      assertTerms(tokens, "9780306406157");
    }

    @Test
    @DisplayName("Valid ISBN-10 with dashes is converted to ISBN-13")
    void isbn10WithDashes_convertedToIsbn13() throws IOException {
      var tokens = analyze(analyzer, "0-306-40615-2");
      assertTerms(tokens, "9780306406157");
    }

    @Test
    @DisplayName("Multiple valid ISBN tokens are each normalized independently")
    void multipleIsbnTokens_allNormalized() throws IOException {
      // Two tokens: an ISBN-10 and an ISBN-13
      var tokens = analyze(analyzer, "0306406152 9780451526533");
      assertTerms(tokens, "9780306406157", "9780451526533");
    }
  }

  // -------------------------------------------------------------------------
  // Token dropping
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Token dropping (echoInvalidInput=false)")
  class TokenDropping {

    @Test
    @DisplayName("Single non-ISBN token is dropped")
    void nonIsbn_dropped() throws IOException {
      var tokens = analyze(analyzer, "notanisbn");
      assertTrue(tokens.isEmpty(), "Expected no tokens; got: " + tokens);
    }

    @Test
    @DisplayName("Non-ISBN tokens among valid ISBNs are dropped; ISBNs pass through")
    void mixedTokens_nonIsbnDropped() throws IOException {
      var tokens = analyze(analyzer, "0306406152 notanisbn 9780451526533");
      assertTerms(tokens, "9780306406157", "9780451526533");
    }

    @Test
    @DisplayName("All non-ISBN tokens result in empty output")
    void allNonIsbn_emptyOutput() throws IOException {
      var tokens = analyze(analyzer, "foo bar baz");
      assertTrue(tokens.isEmpty(), "Expected no tokens; got: " + tokens);
    }
  }

  // -------------------------------------------------------------------------
  // echoInvalidInput
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("echoInvalidInput=true")
  class EchoInvalidInput {

    private Analyzer echoAnalyzer;

    @BeforeEach
    void setUp() {
      var factory = new ISBNNormalizerFilterFactory(Map.of("echoInvalidInput", "true"));
      echoAnalyzer =
          new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
              Tokenizer source = new WhitespaceTokenizer();
              TokenStream result = factory.create(source);
              return new TokenStreamComponents(source, result);
            }
          };
    }

    @AfterEach
    void tearDown() {
      echoAnalyzer.close();
    }

    @Test
    @DisplayName("Non-ISBN token is echoed unchanged")
    void nonIsbn_echoedUnchanged() throws IOException {
      var tokens = analyze(echoAnalyzer, "notanisbn");
      assertTerms(tokens, "notanisbn");
    }

    @Test
    @DisplayName("Valid ISBN token is still normalized (not echoed)")
    void validIsbn_normalizedNotEchoed() throws IOException {
      var tokens = analyze(echoAnalyzer, "0306406152");
      assertTerms(tokens, "9780306406157");
    }

    @Test
    @DisplayName("Mixed: valid ISBNs normalized, non-ISBNs echoed")
    void mixed_isbnNormalizedNonIsbnEchoed() throws IOException {
      var tokens = analyze(echoAnalyzer, "0306406152 notanisbn 9780451526533");
      assertTerms(tokens, "9780306406157", "notanisbn", "9780451526533");
    }
  }
}
