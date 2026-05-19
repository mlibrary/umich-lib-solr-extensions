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
