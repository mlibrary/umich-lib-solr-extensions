// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.filter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenStream;

/**
 * Abstract factory base class for {@link SimpleFilter} subclasses.
 *
 * <p>Handles the standard {@code echoInvalidInput} parameter (boolean, default {@code false}). When
 * {@code true}, tokens for which {@link SimpleFilter#munge} returns {@code null} are passed through
 * unchanged rather than dropped.
 *
 * <p>All other attributes declared in {@code schema.xml} are forwarded to the filter and are
 * accessible via {@link SimpleFilter#getArg(String, String)}. Subclass factories must override
 * {@link #create(TokenStream)} and pass {@link #getFilterArgs()} to the filter constructor:
 *
 * <pre>{@code
 * public class MyFilterFactory extends SimpleFilterFactory {
 *     public static final String NAME = "myFilter";
 *
 *     public MyFilterFactory() {}  // required for ServiceLoader enumeration
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
public abstract class SimpleFilterFactory extends TokenFilterFactory {

  private final boolean echoInvalidInput;

  /**
   * Snapshot of schema.xml args forwarded to the filter, taken after {@code echoInvalidInput} has
   * been consumed from the map.
   */
  private final Map<String, String> filterArgs;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * No-arg constructor required by {@link java.util.ServiceLoader} for SPI enumeration. Concrete
   * subclasses must expose a {@code public} no-arg constructor that delegates here. Not for direct
   * production use.
   */
  protected SimpleFilterFactory() {
    this(new LinkedHashMap<>());
  }

  /**
   * Constructs this factory from the schema.xml attribute map.
   *
   * <p>Makes a defensive mutable copy of {@code args} so that callers may safely pass immutable
   * maps (e.g., {@code Map.of(...)}). Passes the mutable copy to {@link TokenFilterFactory} so
   * Lucene's unknown-argument validation can detect typos in {@code schema.xml}. Consumes {@code
   * echoInvalidInput} via {@link #get(String, String)} so it is removed from the framework's
   * tracked-parameter map and will not be reported as an unknown parameter.
   *
   * @param args attribute map from schema.xml (may be immutable)
   */
  public SimpleFilterFactory(Map<String, String> args) {
    super(new LinkedHashMap<>(args)); // super stores a mutable copy
    // Read and consume echoInvalidInput from the original args.
    String echoStr = args.getOrDefault("echoInvalidInput", null);
    this.echoInvalidInput = Boolean.parseBoolean(echoStr != null ? echoStr : "false");
    // Snapshot remaining args (original minus echoInvalidInput) for forwarding to the filter.
    Map<String, String> snapshot = new LinkedHashMap<>(args);
    snapshot.remove("echoInvalidInput");
    this.filterArgs = Collections.unmodifiableMap(snapshot);
  }

  // -------------------------------------------------------------------------
  // Accessors for subclasses
  // -------------------------------------------------------------------------

  /** Whether invalid tokens are echoed unchanged instead of dropped. */
  public boolean getEchoInvalidInput() {
    return echoInvalidInput;
  }

  /**
   * Returns an immutable snapshot of the schema.xml attributes supplied to this factory (excluding
   * {@code echoInvalidInput}, which is consumed by this base class). Pass this to the {@link
   * SimpleFilter} constructor so {@link SimpleFilter#getArg(String, String)} works in the filter.
   */
  protected Map<String, String> getFilterArgs() {
    return filterArgs;
  }

  // -------------------------------------------------------------------------
  // Abstract creation hook
  // -------------------------------------------------------------------------

  /**
   * Creates a {@link SimpleFilter} wrapping the given token stream. Concrete subclasses must
   * override this method.
   *
   * @param input the upstream token stream
   * @return a new filter instance
   */
  @Override
  public abstract SimpleFilter create(TokenStream input);

  // -------------------------------------------------------------------------
  // Legacy compat helper
  // -------------------------------------------------------------------------

  /**
   * Translates the legacy {@code passThroughOnError} parameter to the {@code echoInvalidInput} key
   * expected by {@link SimpleFilterFactory}. Subclass factories that need to support schemas still
   * using the old attribute name should call this from their constructor before passing {@code
   * args} to {@code super(args)}.
   *
   * @param args the raw args map from schema.xml
   * @return a copy of {@code args} with the key renamed if necessary
   */
  protected static Map<String, String> normalizeArgs(Map<String, String> args) {
    Map<String, String> normalized = new LinkedHashMap<>(args);
    if (normalized.containsKey("passThroughOnError")
        && !normalized.containsKey("echoInvalidInput")) {
      normalized.put("echoInvalidInput", normalized.remove("passThroughOnError"));
    }
    return normalized;
  }
}
