# SimpleFilter — building one-token-in / one-token-out Solr filters

`SimpleFilter` and `SimpleFilterFactory` are the scaffolding used by every normalizer filter in
this library. They handle the Lucene/Solr plumbing so that you only have to write one method:
`munge(String input)`.

---

## How it works

```
upstream token stream
        │
        ▼
  incrementToken()          ← driven by Solr/Lucene
        │
        ▼
   munge(token)
        │
   ┌────┴────────────────┐
   │ null returned?      │
   │                     │
   │ echoInvalidInput=false  → drop token (try next)
   │ echoInvalidInput=true   → pass original token through unchanged
   │                     │
   └─── non-null string ─┴─→ emit transformed token
```

- Return the **transformed string** to emit it.
- Return **`null`** to signal that the token is invalid. The token is then
  dropped (default) or echoed unchanged (`echoInvalidInput="true"`).
- Empty and null tokens from upstream are passed through as-is without
  calling `munge`.

---

## Writing a filter

Extend `SimpleFilter` and override `munge`:

```java
package com.example.solr.filter;

import edu.umich.lib.solr.filter.SimpleFilter;
import org.apache.lucene.analysis.TokenStream;
import java.util.Map;

/**
 * Uppercases every token.  Returns null (drops) if the token contains a digit.
 */
public class UpperCaseFilter extends SimpleFilter {

    public UpperCaseFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
        super(in, echoInvalidInput, args);
        // read optional schema.xml params with getArg("paramName", "default")
    }

    @Override
    public String munge(String input) {
        if (input.chars().anyMatch(Character::isDigit)) {
            return null;   // signals "invalid" — drop or echo per echoInvalidInput
        }
        return input.toUpperCase();
    }
}
```

---

## Writing the factory

Extend `SimpleFilterFactory` and pass the args through:

```java
package com.example.solr.filter;

import edu.umich.lib.solr.filter.SimpleFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import java.util.Map;

public class UpperCaseFilterFactory extends SimpleFilterFactory {

    public static final String NAME = "upperCase";   // SPI alias for schema.xml

    public UpperCaseFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public UpperCaseFilter create(TokenStream in) {
        return new UpperCaseFilter(in, getEchoInvalidInput(), getFilterArgs());
    }
}
```

Register the factory with Lucene's `ServiceLoader` by adding a line to:

```
src/main/resources/META-INF/services/org.apache.lucene.analysis.TokenFilterFactory
```

```
com.example.solr.filter.UpperCaseFilterFactory
```

Once registered the SPI `NAME` alias can be used in `schema.xml`; before registration (or in
tests) use the fully-qualified class name.

---

## Using it in schema.xml

```xml
<!-- echoInvalidInput="true": tokens containing digits are kept as-is -->
<fieldType name="text_upper" class="solr.TextField">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="com.example.solr.filter.UpperCaseFilterFactory"
            echoInvalidInput="true"/>
  </analyzer>
</fieldType>

<!-- echoInvalidInput="false" (default): tokens containing digits are dropped -->
<fieldType name="text_upper_strict" class="solr.TextField">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="com.example.solr.filter.UpperCaseFilterFactory"/>
  </analyzer>
</fieldType>
```

---

## Reading extra parameters from schema.xml

Any attribute you add to the `<filter>` element in `schema.xml` is available inside the filter
via `getArg`:

```xml
<filter class="com.example.solr.filter.UpperCaseFilterFactory"
        echoInvalidInput="true"
        locale="tr"/>
```

```java
public UpperCaseFilter(TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
    super(in, echoInvalidInput, args);
    String locale = getArg("locale", "en");      // optional, default "en"
    String required = getArg("requiredParam");    // throws if absent
}
```

`echoInvalidInput` is consumed by `SimpleFilterFactory` and does **not** appear in `getFilterArgs()` /
`getArg`. All other attributes are forwarded.

---

## Legacy parameter alias

If your schema uses `passThroughOnError` (the name used in the older field type layer), call
`normalizeArgs` in your factory constructor to translate it to `echoInvalidInput` before
passing args to `super`:

```java
public MyFilterFactory(Map<String, String> args) {
    super(normalizeArgs(args));   // normalizeArgs is a protected static on SimpleFilterFactory
}
```
