package edu.umich.lib.normalize.callnumber;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create a "callnumber key" suitable for sorting/searching within libraryIdentifier.
 * <p>
 * Provides tests to see if the given callnumber is considered valid in the
 * implementing schema, and allows for truncated values (which aren't valid
 * but still can be useful, esp. for range queries) creating "invalid" keys,
 * which produce a potentially-useful key despite the fact that the
 * underlying callnumber isn't valid as defined by the implementation.
 *
 * @author Bill Dueber
 */
abstract class AbstractCallNumber {

    /**
     * The passed in string, unmodified
     */
    public String original;
    /**
     * The original string with schema-aware non-semantic leading/trailing characters
     * (e.g., spaces) removed
     */
    public String trimmedOriginal;

    /**
     * Whether this callnumber is considered valid in the implementing scheme
     */
    public Boolean isValid;

    /**
     * @return Whether we can construct a valid key, based on the implementing schema
     */
    abstract Boolean hasValidKey();

    /**
     * Computes a valid key
     *
     * @return
     */
    abstract String validKey();

    /**
     * @see #acceptableTruncatedKey()
     */
    abstract Boolean hasAcceptableTruncatedKey();

    /**
     * Some schemes have "truncated" versions that can be useful for sorting/searching
     * (e.g., an LC call number requires both an alphabetic and numeric portion, but
     * it can be useful to accept just the alphabetic portion.
     *
     * @return
     */
    abstract String acceptableTruncatedKey();

    /**
     * @return The "invalid" key -- a transformed version of the input string
     */
    abstract String invalidKey();

    /**
     * @return The valid key, acceptable truncated key, or null
     */
    public String anyAcceptableKey() {
        if (hasValidKey()) return validKey();
        if (hasAcceptableTruncatedKey()) return acceptableTruncatedKey();
        return null;
    }

    public String bestKey() {
        return bestKey(true, true);
    }


    public String bestKey(Boolean allowTruncated, Boolean passThroughOnError) {
        if (hasValidKey()) return validKey();
        if (allowTruncated && hasAcceptableTruncatedKey()) return acceptableTruncatedKey();
        if (passThroughOnError) return invalidKey();
        return null;
    }

    /**
     * @return The valid/truncated key if available; otherwise the processed "invalid" key
     */
    public String anyKey() {
        String k = anyAcceptableKey();
        if (k == null) {
            return invalidKey();
        } else {
            return k;
        }
    }

    // For trimming punctuation
    public static Pattern trimPunct = Pattern.compile(
            "^\\p{Punct}*(.*?)\\p{Punct}*$"
    );

    public String trimPunctuation(String str) {
        Matcher m = trimPunct.matcher(str);
        if (m.matches()) {
            return m.group(1);
        } else {
            return str;
        }
    }

    public String collapseSpaces(String str) {
        return str.trim().replaceAll("\\s+", " ");
    }


}
