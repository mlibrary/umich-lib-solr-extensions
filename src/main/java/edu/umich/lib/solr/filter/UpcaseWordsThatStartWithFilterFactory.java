package edu.umich.lib.solr.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Factory for {@link UpcaseWordsThatStartWithFilter}.
 *
 * <p>Required parameter: {@code letter} — a single alphabetic character.
 * Optional parameter: {@code reverse} (boolean, default {@code false}).
 *
 * <p>The {@code echoInvalidInput} parameter has no practical effect because
 * {@link UpcaseWordsThatStartWithFilter#munge(String)} never returns {@code null}.
 *
 * <h2>Usage example (schema.xml)</h2>
 * <pre>{@code
 * <filter class="solr.UpcaseWordsThatStartWithFilterFactory"
 *         letter="a" reverse="false"/>
 * }</pre>
 */
public class UpcaseWordsThatStartWithFilterFactory extends SimpleFilterFactory {

    /** SPI name used in schema.xml and for ServiceLoader registration. */
    public static final String NAME = "upcaseWordsThatStartWith";

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     * Direct instantiation without a configuration map is not supported.
     */
    public UpcaseWordsThatStartWithFilterFactory() {
        throw new UnsupportedOperationException(
                "Use UpcaseWordsThatStartWithFilterFactory(Map<String,String>) instead");
    }

    /**
     * @param args schema.xml attributes; must contain {@code letter}
     * @throws IllegalArgumentException if {@code letter} is absent, not exactly
     *                                  one character, or not an alphabetic letter
     */
    public UpcaseWordsThatStartWithFilterFactory(Map<String, String> args) {
        super(args);
        // Validate 'letter' at construction time so schema.xml errors surface early.
        String raw = args.get("letter");
        if (raw == null) {
            throw new IllegalArgumentException("UpcaseWordsThatStartWithFilterFactory requires a 'letter' argument");
        }
        if (raw.length() != 1) {
            throw new IllegalArgumentException(
                    "UpcaseWordsThatStartWithFilterFactory 'letter' must be exactly one character; got: \"" + raw + "\"");
        }
        if (!Character.isLetter(raw.charAt(0))) {
            throw new IllegalArgumentException(
                    "UpcaseWordsThatStartWithFilterFactory 'letter' must be an alphabetic letter; got: \"" + raw + "\"");
        }
    }

    @Override
    public UpcaseWordsThatStartWithFilter create(TokenStream in) {
        return new UpcaseWordsThatStartWithFilter(in, getEchoInvalidInput(), getFilterArgs());
    }
}
