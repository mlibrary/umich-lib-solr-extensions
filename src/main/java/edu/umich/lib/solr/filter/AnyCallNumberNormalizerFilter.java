package edu.umich.lib.solr.filter;

import edu.umich.lib.normalize.callnumber.AnyCallNumberSimple;
import org.apache.lucene.analysis.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * A Solr/Lucene token filter that normalizes LC or Dewey call number tokens to
 * a sortable key string suitable for sort fields and (when combined with an
 * edge-ngram filter) left-anchored prefix search.
 *
 * <p>Tokens that cannot be parsed as a call number return {@code null} from
 * {@link #munge(String)}, which causes {@link SimpleFilter} to either drop the
 * token or echo it unchanged, depending on the {@code echoInvalidInput} setting.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public final class AnyCallNumberNormalizerFilter extends SimpleFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final boolean allowTruncated;

    /**
     * Full constructor used by {@link AnyCallNumberNormalizerFilterFactory}.
     *
     * @param in               the upstream token stream
     * @param echoInvalidInput when {@code true}, tokens that cannot be parsed
     *                         are passed through unchanged
     * @param args             all schema.xml attributes forwarded from the factory
     */
    public AnyCallNumberNormalizerFilter(TokenStream in,
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
    public AnyCallNumberNormalizerFilter(TokenStream in,
                                         boolean allowTruncated,
                                         boolean echoInvalidInput) {
        super(in, echoInvalidInput);
        this.allowTruncated = allowTruncated;
    }

    /** Default constructor: does not allow truncated keys, drops invalid tokens. */
    public AnyCallNumberNormalizerFilter(TokenStream in) {
        this(in, false, false);
    }

    /**
     * Normalizes a call number string to a sortable key.
     *
     * @param input the raw call number token text
     * @return the normalized key, or {@code null} if the input cannot be parsed
     */
    @Override
    public String munge(String input) {
        try {
            AnyCallNumberSimple cn = new AnyCallNumberSimple(input);
            String key = cn.bestKey(allowTruncated, false);
            if (key == null) {
                LOGGER.warn("No best key for '{}'", input);
            }
            return key;
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage(), e);
            return null;
        }
    }
}
