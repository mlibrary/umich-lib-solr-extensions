package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Factory for {@link LCCNNormalizerFilter}.
 *
 * <p>Supports the {@code echoInvalidInput} parameter (boolean, default {@code false}),
 * though in practice {@link LCCNNormalizerFilter#munge(String)} never returns
 * {@code null} so the parameter has no effect.
 */
public class LCCNNormalizerFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "lccnNormalizer";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public LCCNNormalizerFilterFactory() {
        throw new UnsupportedOperationException(
                "Use LCCNNormalizerFilterFactory(Map<String,String>) instead");
    }

    public LCCNNormalizerFilterFactory(Map<String, String> args) {
        super(args);
    }

    /** {@inheritDoc} Creates an {@link LCCNNormalizerFilter}. */
    @Override
    public LCCNNormalizerFilter create(TokenStream in) {
        return new LCCNNormalizerFilter(in, getEchoInvalidInput(), getFilterArgs());
    }
}
