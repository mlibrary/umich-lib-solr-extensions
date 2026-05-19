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
- Return **`null`** to signal that the token is **invalid** and should be discarded.
  The token is then dropped (default) or echoed unchanged (`echoInvalidInput="true"`).
- Empty and null tokens from upstream are passed through as-is without calling `munge`.

> **`null` means "invalid", not "no change needed".**  
> To pass a token through untransformed, return the original string.
> Returning `null` is for cases where the token is genuinely bad — unparseable ISBNs,
> call numbers that cannot be normalized, etc.

---

## Example: `UpcaseWordsThatStartWith`

This filter uppercases every token that begins with a configurable letter and leaves all
other tokens unchanged. A required schema.xml attribute `letter` supplies the letter.

### Design notes

**`munge` returns the original string for non-matching tokens, not `null`.**  
Returning `null` would drop them from the token stream. The `null`/drop contract is for
genuinely invalid input, not for "this token didn't match my predicate".

**`letter` is validated in the constructor.**  
If the attribute is missing or is not a single letter, the filter throws immediately at
Solr core load time — before any documents are indexed — with a descriptive message.

**`echoInvalidInput` has no effect here.**  
Because `munge` never returns `null`, there is nothing to drop or echo. The parameter is
harmless but irrelevant. Filters where all tokens are either transformed or passed through
unchanged do not need `echoInvalidInput`.

**The letter comparison is case-insensitive.**  
`letter="a"` matches tokens starting with both `"a"` and `"A"`.

### Filter class

```java
package com.example.solr.filter;

import edu.umich.lib.solr.filter.SimpleFilter;
import org.apache.lucene.analysis.TokenStream;
import java.util.Map;

/**
 * Uppercases tokens that begin with a configured letter; all other tokens pass
 * through unchanged.
 *
 * <p>Required schema.xml attribute: {@code letter} — a single alphabetic character.
 * <p>Optional schema.xml attribute: {@code reverse} (default {@code false}) — when
 * {@code true}, the characters of the output are reversed before returning.
 *
 * <p>Example: {@code letter="a" reverse="false"} → {@code "apple"} → {@code "APPLE"}
 * <p>Example: {@code letter="a" reverse="true"}  → {@code "apple"} → {@code "ELPPA"}
 */
public class UpcaseWordsThatStartWithFilter extends SimpleFilter {

    private final char letter;
    private final boolean reverse;

    public UpcaseWordsThatStartWithFilter(
            TokenStream in, boolean echoInvalidInput, Map<String, String> args) {
        super(in, echoInvalidInput, args);

        String raw = getArg("letter");   // throws if absent
        if (raw.length() != 1 || !Character.isLetter(raw.charAt(0))) {
            throw new IllegalArgumentException(
                    "UpcaseWordsThatStartWithFilter: 'letter' must be a single alphabetic "
                    + "character, got: \"" + raw + "\"");
        }
        this.letter = Character.toLowerCase(raw.charAt(0));
        this.reverse = Boolean.parseBoolean(getArg("reverse", "false"));
    }

    @Override
    public String munge(String input) {
        if (Character.toLowerCase(input.charAt(0)) == letter) {
            String result = input.toUpperCase();
            return reverse ? new StringBuilder(result).reverse().toString() : result;
        }
        return input;   // pass through unchanged — do NOT return null
    }
}
```

### Factory class

```java
package com.example.solr.filter;

import edu.umich.lib.solr.filter.SimpleFilterFactory;
import org.apache.lucene.analysis.TokenStream;
import java.util.Map;

public class UpcaseWordsThatStartWithFilterFactory extends SimpleFilterFactory {

    public static final String NAME = "upcaseWordsThatStartWith";

    public UpcaseWordsThatStartWithFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public UpcaseWordsThatStartWithFilter create(TokenStream in) {
        return new UpcaseWordsThatStartWithFilter(in, getEchoInvalidInput(), getFilterArgs());
    }
}
```

Register in:

```
src/main/resources/META-INF/services/org.apache.lucene.analysis.TokenFilterFactory
```

```
com.example.solr.filter.UpcaseWordsThatStartWithFilterFactory
```

### schema.xml

```xml
<!-- Uppercase tokens starting with "a"; leave all others alone -->
<fieldType name="text_upcase_a" class="solr.TextField">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="com.example.solr.filter.UpcaseWordsThatStartWithFilterFactory"
            letter="a"/>
  </analyzer>
</fieldType>

<!-- Same, but also reverse the characters of matching tokens -->
<fieldType name="text_upcase_a_reversed" class="solr.TextField">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="com.example.solr.filter.UpcaseWordsThatStartWithFilterFactory"
            letter="a" reverse="true"/>
  </analyzer>
</fieldType>
```

`"apples and bananas"` with `letter="a"`:
- `reverse="false"` (default) → `[APPLES, and, bananas]`
- `reverse="true"` → `[SELPPA, and, bananas]`

---

## Reading extra parameters from schema.xml

Any attribute on the `<filter>` element is available inside the filter via `getArg`:

| Method | Behaviour |
|--------|-----------|
| `getArg("key")` | Returns the value; throws `IllegalArgumentException` if absent |
| `getArg("key", "default")` | Returns the value, or `"default"` if absent; never returns `null` |

`echoInvalidInput` is consumed by `SimpleFilterFactory` and does **not** appear in
`getFilterArgs()` / `getArg`. All other attributes are forwarded.

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
