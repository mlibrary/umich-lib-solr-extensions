package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for simple one-token-in / one-token-out filters.
 * Subclasses override {@link #munge(String)} to transform each token string.
 *
 * <p>If {@code munge} returns {@code null} the token is treated as invalid:
 * it is dropped by default, or echoed unchanged when {@code echoInvalidInput}
 * is {@code true}.
 *
 * <h2>Passing schema.xml parameters to {@code munge}</h2>
 * <p>When instantiated via {@link SimpleFilterFactory}, all attributes declared
 * in {@code schema.xml} are available through {@link #getArg(String, String)}.
 * Subclasses should read parameters in their constructor:
 *
 * <pre>{@code
 * public class MyFilter extends SimpleFilter {
 *     private final String myParam;
 *
 *     public MyFilter(TokenStream in, boolean echoInvalidInput, Map<String,String> args) {
 *         super(in, echoInvalidInput, args);
 *         this.myParam = getArg("myParam", "defaultValue");
 *     }
 *
 *     @Override
 *     public String munge(String input) { ... }
 * }
 * }</pre>
 *
 * <p>Migrated from edu-umich-lib-solrMegapack pluginScaffold.
 */
public class SimpleFilter extends TokenFilter {

    private final CharTermAttribute myTermAttribute =
            addAttribute(CharTermAttribute.class);

    private final boolean echoInvalidInput;

    /** Immutable snapshot of all schema.xml args passed from the factory. */
    private final Map<String, String> filterArgs;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Full constructor used by {@link SimpleFilterFactory} subclasses.
     *
     * @param in               the upstream token stream
     * @param echoInvalidInput when {@code true}, tokens for which {@link #munge}
     *                         returns {@code null} are passed through unchanged
     * @param args             all schema.xml attributes; available via
     *                         {@link #getArg(String, String)}
     */
    public SimpleFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
        super(in);
        this.echoInvalidInput = echoInvalidInput;
        this.filterArgs = Collections.unmodifiableMap(new HashMap<>(args));
    }

    /**
     * Convenience constructor for direct use without schema.xml args.
     */
    public SimpleFilter(TokenStream in, boolean echoInvalidInput) {
        this(in, echoInvalidInput, Map.of());
    }

    /**
     * Convenience constructor; {@code echoInvalidInput} defaults to {@code false}.
     */
    public SimpleFilter(TokenStream in) {
        this(in, false, Map.of());
    }

    // -------------------------------------------------------------------------
    // Arg access for subclasses
    // -------------------------------------------------------------------------

    /**
     * Returns the value of a required schema.xml attribute.
     *
     * @param key the attribute name
     * @return the attribute value
     * @throws IllegalArgumentException if the attribute was not present in
     *                                  the schema.xml configuration
     */
    protected String getArg(String key) {
        String value = filterArgs.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Required filter argument '" + key + "' was not provided in schema.xml");
        }
        return value;
    }

    /**
     * Returns the value of an optional schema.xml attribute, or the supplied
     * default when the attribute is absent.  Subclasses must always supply a
     * meaningful default — this method never returns {@code null}.
     *
     * @param key          the attribute name
     * @param defaultValue the value to use when the attribute is absent
     * @return the attribute value, or {@code defaultValue}
     */
    protected String getArg(String key, String defaultValue) {
        return filterArgs.getOrDefault(key, defaultValue);
    }

    // -------------------------------------------------------------------------
    // Core filter logic
    // -------------------------------------------------------------------------

    /**
     * Transform the input token string.  Return {@code null} to signal that
     * the token is invalid (it will be dropped or echoed depending on
     * {@code echoInvalidInput}).
     */
    public String munge(String input) {
        return input;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            String t = myTermAttribute.toString();
            if (t == null || t.isEmpty()) {
                return true;
            }

            myTermAttribute.setEmpty();
            String newStr = munge(t);
            if (newStr == null) {
                if (echoInvalidInput) {
                    myTermAttribute.append(t);
                    return true;
                }
                // else: drop this token and try the next one
            } else {
                myTermAttribute.append(newStr);
                return true;
            }
        }
        return false;
    }
}
