package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Map;

/**
 * Factory for {@link LCCallNumberSimpleFilter}.
 *
 * <p>Normalizes LC call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code true}) — when {@code true},
 *       truncated call number keys are accepted.</li>
 *   <li>{@code passThroughOnError} (boolean, default {@code false}) — when {@code true},
 *       tokens that cannot be parsed as an LC call number are passed through unchanged.</li>
 * </ul>
 *
 * <h2>Schema example</h2>
 * <pre>{@code
 * <fieldType name="text_lccallnumber" class="solr.TextField">
 *   <analyzer type="index">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberSimple" passThroughOnError="true"/>
 *     <filter class="solr.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 *   </analyzer>
 *   <analyzer type="query">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="lcCallNumberSimple" passThroughOnError="true"/>
 *   </analyzer>
 * </fieldType>
 * }</pre>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class LCCallNumberSimpleFilterFactory extends TokenFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "lcCallNumberSimple";

    private Boolean allowTruncated;
    private Boolean passThroughOnError;

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public LCCallNumberSimpleFilterFactory() {
        throw new UnsupportedOperationException(
                "Use LCCallNumberSimpleFilterFactory(Map<String,String>) instead");
    }

    public LCCallNumberSimpleFilterFactory(Map<String, String> args) {
        super(args);
        allowTruncated = Boolean.parseBoolean(args.getOrDefault("allowTruncated", String.valueOf(true)));
        passThroughOnError = Boolean.parseBoolean(args.getOrDefault("passThroughOnError", String.valueOf(false)));

    }

    @Override
    public LCCallNumberSimpleFilter create(TokenStream input) {
        return new LCCallNumberSimpleFilter(input, allowTruncated, passThroughOnError);
    }
}
