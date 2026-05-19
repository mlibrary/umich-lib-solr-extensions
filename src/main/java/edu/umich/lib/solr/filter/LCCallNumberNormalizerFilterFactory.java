package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Factory for {@link LCCallNumberNormalizerFilter}.
 *
 * <p>Normalizes LC call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code false}) — when {@code true},
 *       truncated call number keys are accepted.</li>
 *   <li>{@code echoInvalidInput} (boolean, default {@code false}) — when {@code true},
 *       tokens that cannot be parsed as an LC call number are passed through unchanged.</li>
 * </ul>
 *
 * <h2>Schema example</h2>
 * <pre>{@code
 * <fieldType name="text_lccallnumber" class="solr.TextField">
 *   <analyzer type="index">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberNormalizer" echoInvalidInput="true"/>
 *     <filter class="solr.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 *   </analyzer>
 *   <analyzer type="query">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberNormalizer" echoInvalidInput="true"/>
 *   </analyzer>
 * </fieldType>
 * }</pre>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class LCCallNumberNormalizerFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "lcCallNumberNormalizer";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public LCCallNumberNormalizerFilterFactory() {
        throw new UnsupportedOperationException(
                "Use LCCallNumberNormalizerFilterFactory(Map<String,String>) instead");
    }

    public LCCallNumberNormalizerFilterFactory(Map<String, String> args) {
        super(normalizeArgs(args));
    }

    @Override
    public LCCallNumberNormalizerFilter create(TokenStream input) {
        return new LCCallNumberNormalizerFilter(input, getEchoInvalidInput(), getFilterArgs());
    }
}
