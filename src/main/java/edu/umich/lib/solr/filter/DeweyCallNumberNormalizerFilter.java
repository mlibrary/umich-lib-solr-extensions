package edu.umich.lib.solr.filter;

import edu.umich.lib.normalize.callnumber.DeweySimple;
import org.apache.lucene.analysis.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * A Solr/Lucene token filter that normalizes Dewey call number tokens to a
 * sortable key string suitable for both sort fields and left-anchored prefix
 * search (when paired with an edge-ngram filter).
 *
 * <p>Tokens that cannot be parsed as a Dewey call number return {@code null}
 * from {@link #munge(String)}, which causes {@link SimpleFilter} to either
 * drop the token or echo it unchanged, depending on the {@code echoInvalidInput} setting.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public final class DeweyCallNumberNormalizerFilter extends SimpleFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final boolean allowTruncated;

    /**
     * Full constructor used by {@link DeweyCallNumberNormalizerFilterFactory}.
     *
     * @param in               the upstream token stream
     * @param echoInvalidInput when {@code true}, tokens that cannot be parsed
     *                         are passed through unchanged
     * @param args             all schema.xml attributes forwarded from the factory
     */
    public DeweyCallNumberNormalizerFilter(TokenStream in,
                                           boolean echoInvalidInput,
                                           Map<String, String> args) {
        super(in, echoInvalidInput, args);
        this.allowTruncated = Boolean.parseBoolean(getArg("allowTruncated", "false"));
    }

    /**
     * Convenience constructor for direct use without a factory.
     *
     * @param in               the upstream token stream
     * @param allowTruncated   when {@code true}, truncated call number keys are accepted
     * @param echoInvalidInput when {@code true}, unparseable tokens are echoed unchanged
     */
    public DeweyCallNumberNormalizerFilter(TokenStream in,
                                           boolean allowTruncated,
                                           boolean echoInvalidInput) {
        super(in, echoInvalidInput);
        this.allowTruncated = allowTruncated;
    }

    /** Default constructor: does not allow truncated keys, drops invalid tokens. */
    public DeweyCallNumberNormalizerFilter(TokenStream in) {
        this(in, false, false);
    }

    /**
     * Normalizes a Dewey call number string to a sortable key.
     *
     * @param input the raw call number token text
     * @return the normalized key, or {@code null} if the input cannot be parsed
     */
    @Override
    public String munge(String input) {
        try {
            DeweySimple dewey = new DeweySimple(input);
            return dewey.bestKey(allowTruncated, false);
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage(), e);
            return null;
        }
    }
}
