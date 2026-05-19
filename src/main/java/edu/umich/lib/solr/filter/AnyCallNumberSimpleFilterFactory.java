package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * @author dueberb
 * A Solr filter that take an LC/Dewey Call Number (/ shelf key) and
 * turns it into something that can be sorted correctly. While the
 * fieldType () is better for general use, if you want to do prefix
 * queries, you need to have an analysis chain so you can add the
 * edge ngram filter, so we've got this.
 * <p>
 *
 * <fieldType name="callnumber_prefix_search"  class="libraryIdentifier.TextField">
 * <analyzer type="index">
 * <tokenizer class="libraryIdentifier.KeywordTokenizerFactory"/>
 * <filter class="edu.umich.library.lucene.analysis.AnyCallNumberSimpleFilterFactory" passThroughOnError="true"/>
 * <filter class="libraryIdentifier.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 * </analyzer>
 * <analyzer type="query">
 * <tokenizer class="libraryIdentifier.KeywordTokenizerFactory"/>
 * <filter class="edu.umich.library.lucene.analysis.AnyCallNumberSimpleFilterFactory" passThroughOnError="true"/>
 * </analyzer>
 * </fieldType>
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
