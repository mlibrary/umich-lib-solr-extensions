package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for {@link AnyCallNumberNormalizerFilter}.
 *
 * <p>Normalizes LC or Dewey call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code false}) — when {@code true},
 *       truncated call number keys are accepted.</li>
 *   <li>{@code passThroughOnError} (boolean, default {@code false}) — when {@code true},
 *       tokens that cannot be parsed as a call number are passed through unchanged.</li>
 * </ul>
 *
 * <h2>Schema example</h2>
 * <pre>{@code
 * <fieldType name="text_anycallnumber" class="solr.TextField">
 *   <analyzer type="index">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="anyCallNumberNormalizer" passThroughOnError="true"/>
 *     <filter class="solr.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 *   </analyzer>
 *   <analyzer type="query">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="anyCallNumberNormalizer" passThroughOnError="true"/>
 *   </analyzer>
 * </fieldType>
 * }</pre>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class AnyCallNumberNormalizerFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "anyCallNumberNormalizer";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public AnyCallNumberNormalizerFilterFactory() {
        throw new UnsupportedOperationException(
                "Use AnyCallNumberNormalizerFilterFactory(Map<String,String>) instead");
    }

    public AnyCallNumberNormalizerFilterFactory(Map<String, String> args) {
        super(normalizeArgs(args));
    }

    @Override
    public AnyCallNumberNormalizerFilter create(TokenStream input) {
        return new AnyCallNumberNormalizerFilter(input, getEchoInvalidInput(), getFilterArgs());
    }

    /**
     * Translates the legacy {@code passThroughOnError} parameter to the
     * {@code echoInvalidInput} key expected by {@link SimpleFilterFactory}.
     */
    private static Map<String, String> normalizeArgs(Map<String, String> args) {
        Map<String, String> normalized = new HashMap<>(args);
        if (normalized.containsKey("passThroughOnError") && !normalized.containsKey("echoInvalidInput")) {
            normalized.put("echoInvalidInput", normalized.remove("passThroughOnError"));
        }
        return normalized;
    }
}
