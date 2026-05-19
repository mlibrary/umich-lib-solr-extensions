package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilterFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory base class for {@link SimpleFilter} subclasses.
 *
 * <p>Supports one built-in configuration parameter:
 * <ul>
 *   <li>{@code echoInvalidInput} (boolean, default {@code false}) — when
 *       {@code true}, tokens for which {@link SimpleFilter#munge(String)}
 *       returns {@code null} are passed through unchanged rather than dropped.
 * </ul>
 *
 * <p>All other attributes declared in {@code schema.xml} are forwarded to the
 * filter and are accessible via {@link SimpleFilter#getArg(String, String)}.
 * Subclass factories should override {@link #create(TokenStream)} and pass
 * {@link #getFilterArgs()} to the filter constructor:
 *
 * <pre>{@code
 * public class MyFilterFactory extends SimpleFilterFactory {
 *     public MyFilterFactory(Map<String,String> args) { super(args); }
 *
 *     @Override
 *     public MyFilter create(TokenStream in) {
 *         return new MyFilter(in, getEchoInvalidInput(), getFilterArgs());
 *     }
 * }
 * }</pre>
 *
 * <p>Migrated from edu-umich-lib-solrMegapack pluginScaffold.
 */
public class SimpleFilterFactory extends TokenFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "simple";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public SimpleFilterFactory() {
        throw new UnsupportedOperationException(
                "Use SimpleFilterFactory(Map<String,String>) instead");
    }

    private final boolean echoInvalidInput;

    /**
     * Snapshot of all schema.xml args (taken before {@code super(args)} may
     * mutate the map), to be forwarded to the filter.
     */
    private final Map<String, String> filterArgs;

    public SimpleFilterFactory(Map<String, String> args) {
        // Pass a copy to super() so any mutation by the parent does not affect
        // our own snapshot of the original caller-supplied attributes.
        super(new HashMap<>(args));
        echoInvalidInput = Boolean.parseBoolean(
                args.getOrDefault("echoInvalidInput", "false"));
        filterArgs = Collections.unmodifiableMap(new HashMap<>(args));
    }

    /** Whether invalid tokens are echoed unchanged instead of dropped. */
    public boolean getEchoInvalidInput() {
        return echoInvalidInput;
    }

    /**
     * Returns an immutable snapshot of all schema.xml attributes supplied to
     * this factory.  Pass this to the {@link SimpleFilter} constructor so
     * {@link SimpleFilter#getArg(String, String)} works in the filter.
     */
    protected Map<String, String> getFilterArgs() {
        return filterArgs;
    }

    @Override
    public SimpleFilter create(TokenStream input) {
        return new SimpleFilter(input, echoInvalidInput, filterArgs);
    }

    /**
     * Translates the legacy {@code passThroughOnError} parameter to the
     * {@code echoInvalidInput} key expected by {@link SimpleFilterFactory}.
     * Subclass factories should call this from their constructor before passing
     * {@code args} to {@code super(args)}.
     *
     * @param args the raw args map from schema.xml
     * @return a copy of {@code args} with the key renamed if necessary
     */
    protected static Map<String, String> normalizeArgs(Map<String, String> args) {
        Map<String, String> normalized = new HashMap<>(args);
        if (normalized.containsKey("passThroughOnError") && !normalized.containsKey("echoInvalidInput")) {
            normalized.put("echoInvalidInput", normalized.remove("passThroughOnError"));
        }
        return normalized;
    }
}
