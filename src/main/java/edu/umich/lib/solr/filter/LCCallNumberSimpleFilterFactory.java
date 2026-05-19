package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Map;

/**
 * @author dueberb
 * A Solr filter that take an LC Call Number (/ shelf key) and
 * turns it into something that can be sorted correctly. While the
 * fieldType () is better for general use, if you want to do prefix
 * queries, you need to have an analysis chain so you can add the
 * edge ngram filter, so we've got this.
 * <p>
 *
 * <fieldType name="callnumber_prefix_search"  class="libraryIdentifier.TextField">
 * <analyzer type="index">
 * <tokenizer class="libraryIdentifier.KeywordTokenizerFactory"/>
 * <filter class="edu.umich.library.lucene.analysis.LCCallNumberSimpleFilterFactory" passThroughOnError="true"/>
 * <filter class="libraryIdentifier.EdgeNGramFilterFactory" maxGramSize="40" minGramSize="2"/>
 * </analyzer>
 * <analyzer type="query">
 * <tokenizer class="libraryIdentifier.KeywordTokenizerFactory"/>
 * <filter class="edu.umich.library.lucene.analysis.LCCallNumberSimpleFilterFactory" passThroughOnError="true"/>
 * </analyzer>
 * </fieldType>
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
