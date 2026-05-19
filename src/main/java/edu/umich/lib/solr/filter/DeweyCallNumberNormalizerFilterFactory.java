package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Factory for {@link DeweyCallNumberNormalizerFilter}.
 *
 * <p>Normalizes Dewey call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code false}) — when {@code true},
 *       truncated call number keys are accepted.</li>
 *   <li>{@code echoInvalidInput} (boolean, default {@code false}) — when {@code true},
 *       tokens that cannot be parsed as a Dewey call number are passed through unchanged.</li>
 * </ul>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class DeweyCallNumberNormalizerFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "deweyCallNumberNormalizer";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public DeweyCallNumberNormalizerFilterFactory() {
        throw new UnsupportedOperationException(
                "Use DeweyCallNumberNormalizerFilterFactory(Map<String,String>) instead");
    }

    public DeweyCallNumberNormalizerFilterFactory(Map<String, String> args) {
        super(normalizeArgs(args));
    }

    @Override
    public DeweyCallNumberNormalizerFilter create(TokenStream input) {
        return new DeweyCallNumberNormalizerFilter(input, getEchoInvalidInput(), getFilterArgs());
    }
}
