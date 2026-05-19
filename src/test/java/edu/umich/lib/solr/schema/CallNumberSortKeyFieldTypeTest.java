package edu.umich.lib.solr.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CallNumberSortKeyFieldType}.
 *
 * <p>{@code toInternal()} and {@code bundledFields()} are pure logic that does not
 * require a live Solr container.  We instantiate the field type directly and rely on
 * its default configuration ({@code allowTruncated=true}, {@code passThroughOnError=false}).
 */
@DisplayName("CallNumberSortKeyFieldType")
class CallNumberSortKeyFieldTypeTest {

    private CallNumberSortKeyFieldType fieldType;

    @BeforeEach
    void setUp() {
        fieldType = new CallNumberSortKeyFieldType();
        // Default config: allowTruncated=true, passThroughOnError=false
    }

    // -----------------------------------------------------------------------
    // bundledFields
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("bundledFields")
    class BundledFieldsTests {

        @Test
        @DisplayName("appends unit-separator and field-delimiter between key and appended value")
        void bundledFieldsFormat() {
            String result = fieldType.bundledFields("ps 3537 a832 b6 1948", "extra");
            // internal form: <normalizedKey> + \u001F + } + appendedField
            assertEquals("ps 3537 a832 b6 1948\u001F}extra", result);
        }

        @Test
        @DisplayName("empty appended field produces trailing delimiter only")
        void bundledFieldsEmptyAppended() {
            String result = fieldType.bundledFields("a 1", "");
            assertEquals("a 1\u001F}", result);
        }

        @Test
        @DisplayName("unit-separator sorts before field-delimiter so short key < extended key")
        void unitSeparatorSortsBefore() {
            // \u001F (31) < } (125) — key alone sorts before key+continuation
            String shorter = fieldType.bundledFields("a 1", "");
            String longer  = fieldType.bundledFields("a 1", " extra");
            assertTrue(shorter.compareTo(longer) < 0,
                "Shorter call number should sort before the same key with appended data");
        }
    }

    // -----------------------------------------------------------------------
    // toInternal — LC call numbers
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toInternal — LC call numbers")
    class LCCallNumbers {

        @Test
        @DisplayName("valid LC call number returns normalised sort key")
        void validLC() {
            String result = fieldType.toInternal("PS3537.A832 B6 1948");
            assertNotNull(result);
            // Key should start with the normalised letter portion (lowercase)
            assertTrue(result.startsWith("ps"), "LC key should be lowercased: " + result);
        }

        @Test
        @DisplayName("two LC call numbers sort correctly via toInternal")
        void lcSortOrder() {
            String a = fieldType.toInternal("A1 .B2");
            String b = fieldType.toInternal("PS3537.A832 B6 1948");
            assertNotNull(a);
            assertNotNull(b);
            assertTrue(a.compareTo(b) < 0, "A1 should sort before PS3537");
        }

        @ParameterizedTest(name = "[{index}] {0} < {1}")
        @DisplayName("LC sort ordering across representative examples")
        @CsvSource({
            "A1 .B2 1900,       PS3537.A832 B6 1948",
            "PS3537.A832 B6 1948, QA76.73.J38 G67 2018",
            "QA76.73.J38 G67 2018, ZZ999 .Z99 9999"
        })
        void lcSortOrdering(String smaller, String larger) {
            String a = fieldType.toInternal(smaller.trim());
            String b = fieldType.toInternal(larger.trim());
            assertNotNull(a, "toInternal returned null for: " + smaller);
            assertNotNull(b, "toInternal returned null for: " + larger);
            assertTrue(a.compareTo(b) < 0,
                "Expected '" + smaller.trim() + "' to sort before '" + larger.trim() + "'");
        }
    }

    // -----------------------------------------------------------------------
    // toInternal — Dewey call numbers
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toInternal — Dewey call numbers")
    class DeweyCallNumbers {

        @Test
        @DisplayName("valid Dewey call number returns a non-null key")
        void validDewey() {
            assertNotNull(fieldType.toInternal("813.54 SAL"));
        }

        @Test
        @DisplayName("two Dewey call numbers sort correctly via toInternal")
        void deweySortOrder() {
            String a = fieldType.toInternal("100 ABC");
            String b = fieldType.toInternal("813.54 SAL");
            assertNotNull(a);
            assertNotNull(b);
            assertTrue(a.compareTo(b) < 0, "100 should sort before 813");
        }
    }

    // -----------------------------------------------------------------------
    // toInternal — field delimiter splitting
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toInternal — field delimiter ( } ) splitting")
    class FieldDelimiterTests {

        @Test
        @DisplayName("value without delimiter produces standard bundled key")
        void noDelimiter() {
            String result = fieldType.toInternal("A1 .B2");
            assertNotNull(result);
            assertTrue(result.contains("\u001F"), "result should contain end-of-callnumber sentinel");
        }

        @Test
        @DisplayName("value with delimiter appends trailing part after normalised key")
        void withDelimiter() {
            String result = fieldType.toInternal("A1 .B2}extra-data");
            assertNotNull(result);
            assertTrue(result.endsWith("}extra-data"),
                "appended part should follow the field delimiter: " + result);
        }

        @Test
        @DisplayName("call-number-only key sorts before same key with appended field")
        void keyAloneSortsBeforeWithAppended() {
            String base     = fieldType.toInternal("A1 .B2");
            String extended = fieldType.toInternal("A1 .B2}zz");
            assertNotNull(base);
            assertNotNull(extended);
            assertTrue(base.compareTo(extended) < 0,
                "base key should sort before extended: base=" + base + " extended=" + extended);
        }
    }

    // -----------------------------------------------------------------------
    // toInternal — truncated / invalid inputs (default config)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toInternal — truncated and invalid inputs")
    class TruncatedAndInvalidInputs {

        @Test
        @DisplayName("letters-only LC stub is accepted as truncated key (allowTruncated=true default)")
        void truncatedLC() {
            // "PS" alone is a valid truncated LC key
            assertNotNull(fieldType.toInternal("PS"));
        }

        @Test
        @DisplayName("letters-only Dewey-matching input not parseable as truncated returns null")
        void invalidInputReturnsNull() {
            // Fully unparseable, passThroughOnError=false → null
            assertNull(fieldType.toInternal("!@#$%"));
        }

        @Test
        @DisplayName("passThroughOnError=true returns invalidKey for unparseable input")
        void passThroughOnError() {
            CallNumberSortKeyFieldType lenient = new CallNumberSortKeyFieldType();
            lenient.passThroughOnError = true;
            assertNotNull(lenient.toInternal("!@#$%"),
                "passThroughOnError=true should return a non-null key even for garbage input");
        }

        @Test
        @DisplayName("allowTruncated=false rejects truncated-only keys")
        void noTruncation() {
            CallNumberSortKeyFieldType strict = new CallNumberSortKeyFieldType();
            strict.allowTruncated = false;
            // "PS" alone has no valid full key, only a truncated key — should return null
            assertNull(strict.toInternal("PS"),
                "allowTruncated=false should reject truncated-only input");
        }
    }
}
