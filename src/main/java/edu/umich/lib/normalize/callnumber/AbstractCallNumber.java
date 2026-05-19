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
public abstract class AbstractCallNumber {

    /**
     * The passed in string, unmodified
     */
    protected String original;
    /**
     * The original string with schema-aware non-semantic leading/trailing characters
     * (e.g., spaces) removed
     */
    protected String trimmedOriginal;

    /**
     * Whether this callnumber is considered valid in the implementing scheme
     */
    protected Boolean isValid;

    /**
     * @return {@code true} if a valid key can be constructed from this call number
     */
    abstract Boolean hasValidKey();

    /**
     * Computes a valid key for this call number.
     *
     * @return the valid sort key, or {@code null} if the call number is not valid
     */
    abstract String validKey();

    /**
     * @return {@code true} if an acceptable truncated key can be produced
     * @see #acceptableTruncatedKey()
     */
    abstract Boolean hasAcceptableTruncatedKey();

    /**
     * Some schemes have "truncated" versions that can be useful for sorting/searching
     * (e.g., an LC call number requires both an alphabetic and numeric portion, but
     * it can be useful to accept just the alphabetic portion.
     *
     * @return the acceptable truncated key, or {@code null} if none is available
     */
    abstract String acceptableTruncatedKey();

    /**
     * Returns the "invalid" key — a transformed version of the input string
     * that can still be used for sorting/searching even when the call number
     * does not conform to the implementing scheme.
     *
     * @return a non-null transformed key string
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


    public String bestKey(boolean allowTruncated, boolean passThroughOnError) {
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
    public static Pattern TRIM_PUNCT = Pattern.compile(
            "^\\p{Punct}*(.*?)\\p{Punct}*$"
    );

    public String trimPunctuation(String str) {
        Matcher m = TRIM_PUNCT.matcher(str);
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
