package edu.umich.lib.solr.filter;

import edu.umich.lib.normalize.lccn.LCCNNormalizer;
import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Solr/Lucene token filter that normalizes LCCN strings.
 *
 * <p>{@link LCCNNormalizer#normalize(String)} handles all inputs gracefully;
 * it never throws and returns the input unchanged when no LCCN pattern is found.
 * Consequently this filter always passes tokens through (normalized or as-is)
 * and never drops them.
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public final class LCCNNormalizerFilter extends SimpleFilter {

    /**
     * Factory-only constructor.  Instantiate via {@link LCCNNormalizerFilterFactory}.
     *
     * @param in               the upstream token stream
     * @param echoInvalidInput forwarded to {@link SimpleFilter}; effectively unused
     *                         because {@code munge} never returns {@code null}
     * @param args             schema.xml attributes forwarded from the factory
     */
    LCCNNormalizerFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
        super(in, echoInvalidInput, args);
    }

    /**
     * Normalizes {@code input} as an LCCN.  Always returns a non-null string;
     * inputs that are not LCCNs are returned unchanged.
     */
    @Override
    public String munge(String input) {
        return LCCNNormalizer.normalize(input);
    }
}
