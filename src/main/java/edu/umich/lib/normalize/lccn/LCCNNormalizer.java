package edu.umich.lib.normalize.lccn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class for normalizing Library of Congress Control Number (LCCN) strings.
 *
 * <p>Follows the normalization algorithm described at
 * <a href="http://www.loc.gov/marc/lccn-namespace.html#syntax">
 * http://www.loc.gov/marc/lccn-namespace.html#syntax</a>.
 *
 * <p>{@link #normalize(String)} always returns a non-null string.  Inputs that
 * do not match any recognized LCCN pattern are returned lowercased with whitespace
 * stripped, rather than throwing.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public final class LCCNNormalizer {

    /** Utility class — do not instantiate. */
    private LCCNNormalizer() {}

    // Normalization patterns from http://www.loc.gov/marc/lccn-namespace.html#syntax
    public static final Pattern trailingSlashPattern = Pattern.compile("^(.*?)/.*$");
    public static final Pattern lccnDashPattern = Pattern.compile("^(\\w+)(?:-(\\d+))?.*$");
    public static final String LOC_GOV_PREFIX = "http://lccn.loc.gov/";

    /**
     * Normalizes a raw LCCN string according to the LOC normalization algorithm.
     *
     * <p>Strips leading {@code http://lccn.loc.gov/} URIs, removes whitespace, lowercases,
     * drops everything after a slash, and zero-pads post-hyphen digits to six places.
     * Inputs that do not match a recognized LCCN pattern are returned lowercased with
     * whitespace stripped rather than throwing.
     *
     * @param raw the raw LCCN string to normalize; must not be {@code null}
     * @return a non-null normalized LCCN string
     */
    public static String normalize(String raw) {

        // Strip lccn.loc.gov URI prefix before any other processing
        if (raw.startsWith(LOC_GOV_PREFIX)) {
            raw = raw.substring(LOC_GOV_PREFIX.length());
        }

        // First, ditch all the spaces and lowercase
        raw = raw.replaceAll("\\s+", "").toLowerCase();

        // Lose everything after a slash
        Matcher m = trailingSlashPattern.matcher(raw);
        if (m.matches()) {
            raw = m.group(1);
        }

        // See if it's even viable. If not, return as-is
        m = lccnDashPattern.matcher(raw);
        if (!(m.matches())) {
            return raw;
        }

        // Otherwise, build it up.
        String prefix = m.group(1);
        String postHyphenDigits = m.group(2);

        // If there wasn't a hyphen, just return the prefix
        if (postHyphenDigits == null || postHyphenDigits.length() == 0) {
            return prefix;
        }

        // If we get here, there *were* digits after the hyphen; we may need to build
        // them out to six digits (zero-padded on the left)

        if (postHyphenDigits.length() < 6) {
            postHyphenDigits = String.format("%06d", Integer.parseInt(postHyphenDigits));
        }
        return prefix + postHyphenDigits;

    }


}
