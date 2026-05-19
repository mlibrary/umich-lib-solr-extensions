package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Factory for {@link AnyCallNumberSimpleFilter}.
 *
 * <p>Normalizes LC or Dewey call number tokens to a sortable key string, suitable
 * for sort fields and (when combined with an edge-ngram filter) left-anchored
 * prefix search.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code allowTruncated} (boolean, default {@code true}) — when {@code true},
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
 *     <filter class="anyCallNumberSimple" passThroughOnError="true"/>
 *     <filter class="solr.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 *   </analyzer>
 *   <analyzer type="query">
 *     <tokenizer class="solr.KeywordTokenizerFactory"/>
 *     <filter class="anyCallNumberSimple" passThroughOnError="true"/>
 *   </analyzer>
 * </fieldType>
 * }</pre>
 *
 * @author Bill Dueber dueberb@umich.edu
 */
public class AnyCallNumberSimpleFilterFactory extends TokenFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "anyCallNumberSimple";

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Boolean allowTruncated;
    private Boolean passThroughOnError;

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public AnyCallNumberSimpleFilterFactory() {
        throw new UnsupportedOperationException(
                "Use AnyCallNumberSimpleFilterFactory(Map<String,String>) instead");
    }

    public AnyCallNumberSimpleFilterFactory(Map<String, String> args) {
        super(args);
        allowTruncated = Boolean.parseBoolean(args.getOrDefault("allowTruncated", String.valueOf(true)));
        passThroughOnError = Boolean.parseBoolean(args.getOrDefault("passThroughOnError", String.valueOf(false)));
    }

    @Override
    public AnyCallNumberSimpleFilter create(TokenStream input) {
        return new AnyCallNumberSimpleFilter(input, this.allowTruncated, this.passThroughOnError);
    }
}
