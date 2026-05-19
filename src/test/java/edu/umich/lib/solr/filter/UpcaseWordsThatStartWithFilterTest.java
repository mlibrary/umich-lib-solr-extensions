package edu.umich.lib.solr.filter;

import edu.umich.lib.solr.testing.TokenStreamAsserter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;

import static edu.umich.lib.solr.testing.TokenStreamAsserter.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UpcaseWordsThatStartWithFilter} and
 * {@link UpcaseWordsThatStartWithFilterFactory}.
 */
@DisplayName("UpcaseWordsThatStartWithFilter")
class UpcaseWordsThatStartWithFilterTest {

    /** Builds an analyzer with the given factory args. */
    private static Analyzer analyzer(Map<String, String> args) {
        var factory = new UpcaseWordsThatStartWithFilterFactory(args);
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenStream result = factory.create(source);
                return new TokenStreamComponents(source, result);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Basic transformation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token uppercasing")
    class TokenUppercasing {

        @Test
        @DisplayName("Token starting with the configured letter is uppercased")
        void matchingToken_uppercased() throws IOException {
            try (var a = analyzer(Map.of("letter", "a"))) {
                assertTerms(analyze(a, "apple"), "APPLE");
            }
        }

        @Test
        @DisplayName("Token NOT starting with the configured letter is passed through unchanged")
        void nonMatchingToken_unchanged() throws IOException {
            try (var a = analyzer(Map.of("letter", "a"))) {
                assertTerms(analyze(a, "banana"), "banana");
            }
        }

        @Test
        @DisplayName("Mixed stream: matching tokens uppercased, others unchanged")
        void mixedStream_selectiveUppercase() throws IOException {
            try (var a = analyzer(Map.of("letter", "a"))) {
                assertTerms(analyze(a, "apple banana avocado cherry"), "APPLE", "banana", "AVOCADO", "cherry");
            }
        }

        @Test
        @DisplayName("All tokens match — all uppercased")
        void allMatchingTokens_allUppercased() throws IOException {
            try (var a = analyzer(Map.of("letter", "b"))) {
                assertTerms(analyze(a, "bear bird bug"), "BEAR", "BIRD", "BUG");
            }
        }

        @Test
        @DisplayName("No tokens match — all pass through unchanged")
        void noMatchingTokens_allUnchanged() throws IOException {
            try (var a = analyzer(Map.of("letter", "z"))) {
                assertTerms(analyze(a, "apple banana cherry"), "apple", "banana", "cherry");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Case-insensitive matching
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Case-insensitive letter matching")
    class CaseInsensitiveMatching {

        @Test
        @DisplayName("letter='a' matches token starting with uppercase 'A'")
        void lowercaseLetter_matchesUppercaseStart() throws IOException {
            try (var a = analyzer(Map.of("letter", "a"))) {
                assertTerms(analyze(a, "Apple"), "APPLE");
            }
        }

        @Test
        @DisplayName("letter='A' (uppercase) matches token starting with lowercase 'a'")
        void uppercaseLetter_matchesLowercaseStart() throws IOException {
            try (var a = analyzer(Map.of("letter", "A"))) {
                assertTerms(analyze(a, "apple"), "APPLE");
            }
        }
    }

    // -------------------------------------------------------------------------
    // reverse option
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("reverse=true option")
    class ReverseOption {

        @Test
        @DisplayName("reverse=true: matching token is uppercased and reversed")
        void reverseTrue_matchingTokenUppercasedAndReversed() throws IOException {
            try (var a = analyzer(Map.of("letter", "a", "reverse", "true"))) {
                // "apple" → "APPLE" → "ELPPA"
                assertTerms(analyze(a, "apple"), "ELPPA");
            }
        }

        @Test
        @DisplayName("reverse=true: non-matching token is NOT reversed, passed through unchanged")
        void reverseTrue_nonMatchingTokenUnchanged() throws IOException {
            try (var a = analyzer(Map.of("letter", "a", "reverse", "true"))) {
                assertTerms(analyze(a, "banana"), "banana");
            }
        }

        @Test
        @DisplayName("reverse=false (explicit): matching token uppercased but not reversed")
        void reverseFalseExplicit_noReversal() throws IOException {
            try (var a = analyzer(Map.of("letter", "a", "reverse", "false"))) {
                assertTerms(analyze(a, "apple"), "APPLE");
            }
        }

        @Test
        @DisplayName("reverse not specified (default): no reversal")
        void reverseDefault_noReversal() throws IOException {
            try (var a = analyzer(Map.of("letter", "a"))) {
                assertTerms(analyze(a, "apple"), "APPLE");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor argument validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Missing 'letter' arg throws IllegalArgumentException")
        void missingLetter_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UpcaseWordsThatStartWithFilterFactory(Map.of()));
        }

        @Test
        @DisplayName("'letter' with length > 1 throws IllegalArgumentException")
        void letterTooLong_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UpcaseWordsThatStartWithFilterFactory(Map.of("letter", "ab")));
        }

        @Test
        @DisplayName("'letter' that is a digit throws IllegalArgumentException")
        void letterIsDigit_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UpcaseWordsThatStartWithFilterFactory(Map.of("letter", "3")));
        }

        @Test
        @DisplayName("'letter' that is punctuation throws IllegalArgumentException")
        void letterIsPunctuation_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new UpcaseWordsThatStartWithFilterFactory(Map.of("letter", "!")));
        }

        @Test
        @DisplayName("Valid 'letter' arg constructs without exception")
        void validLetter_constructsSuccessfully() {
            assertDoesNotThrow(() -> new UpcaseWordsThatStartWithFilterFactory(Map.of("letter", "x")));
        }
    }
}
