package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Factory for {@link ISBNNormalizerFilter}.
 *
 * <p>Supports the {@code echoInvalidInput} parameter (boolean, default {@code false}).
 * When {@code true}, tokens that contain no recognizable ISBN are passed through
 * unchanged rather than dropped.
 */
public class ISBNNormalizerFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "isbnNormalizer";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public ISBNNormalizerFilterFactory() {
        throw new UnsupportedOperationException(
                "Use ISBNNormalizerFilterFactory(Map<String,String>) instead");
    }

    public ISBNNormalizerFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public ISBNNormalizerFilter create(TokenStream in) {
        return new ISBNNormalizerFilter(in, getEchoInvalidInput(), getFilterArgs());
    }
}
