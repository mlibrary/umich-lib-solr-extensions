package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Map;

/**
 * Factory for {@link DeweyCallNumberSimpleFilter}.
 *
 * <p>Normalizes Dewey call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code true}) — when {@code true},
 *       truncated call number keys are accepted.</li>
 *   <li>{@code passThroughOnError} (boolean, default {@code false}) — when {@code true},
 *       tokens that cannot be parsed as a Dewey call number are passed through unchanged.</li>
 * </ul>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class DeweyCallNumberSimpleFilterFactory extends TokenFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "deweyCallNumberSimple";

    private Boolean allowTruncated;
    private Boolean passThroughOnError;

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public DeweyCallNumberSimpleFilterFactory() {
        throw new UnsupportedOperationException(
                "Use DeweyCallNumberSimpleFilterFactory(Map<String,String>) instead");
    }

    public DeweyCallNumberSimpleFilterFactory(Map<String, String> args) {
        super(args);
        allowTruncated = Boolean.parseBoolean(args.getOrDefault("allowTruncated", String.valueOf(true)));
        passThroughOnError = Boolean.parseBoolean(args.getOrDefault("passThroughOnError", String.valueOf(false)));

    }

    @Override
    public DeweyCallNumberSimpleFilter create(TokenStream input) {
        return new DeweyCallNumberSimpleFilter(input, allowTruncated, passThroughOnError);
    }
}
