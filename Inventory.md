# Component Inventory

---

## Infrastructure

| Class | Use Case |
|-------|----------|
| `SimpleFilter` | Abstract base for 1-in/1-out token transforms; subclasses implement `munge(String)`. Supports `echoInvalidInput` to pass bad tokens through instead of dropping them. |
| `SimpleFilterFactory` | Companion factory base; reads `echoInvalidInput` from schema args and forwards all args to the filter constructor. |

---

## Call Number

### CallNumberSortableFieldType

Extends `StrField`; **stores the original value** but **indexes the collation sort key** produced by
`AnyCallNumberSimple`. The indexed key enables exact-match, range, and sort queries over both LC and
Dewey call numbers without the caller needing to know which scheme is in use. Use this when you also
need to display the raw call number (e.g., a browseable call-number field). Fields are typically
`multiValued="true"` to support records with multiple call numbers.

`passThroughOnError="true"` keeps records that have unrecognizable call numbers — the raw value is
stored/indexed as-is, which keeps the record visible but may produce unexpected sort order.
`passThroughOnError="false"` (the default) silently omits the field for unrecognizable values.

```xml
<!-- Lenient: unrecognised call numbers pass through as raw strings -->
<fieldType name="callnumber_sortable"
           class="edu.umich.lib.solr.schema.CallNumberSortableFieldType"
           allowTruncated="true" passThroughOnError="true"
           multiValued="true" stored="true"/>

<!-- Strict: unrecognised call numbers produce no field at all -->
<fieldType name="callnumber_sortable_strict"
           class="edu.umich.lib.solr.schema.CallNumberSortableFieldType"
           allowTruncated="false" passThroughOnError="false"
           multiValued="true" stored="true"/>

<field name="callnumber" type="callnumber_sortable" indexed="true" stored="true"/>
```

---

### CallNumberSortKeyFieldType

Like `CallNumberSortableFieldType` but **both stored and indexed values are the sort key**. The
original call number string is not preserved. Use this when you only need the field for sorting or
range queries and do not need to display the original value. Handy as a dedicated sort-only field
alongside a stored display field.

```xml
<!-- The sort key is what gets stored AND indexed -->
<fieldType name="callnumber_sortkey"
           class="edu.umich.lib.solr.schema.CallNumberSortKeyFieldType"
           allowTruncated="true" passThroughOnError="false"
           multiValued="true" stored="true"/>

<field name="callnumber_sort" type="callnumber_sortkey" indexed="true" stored="true"/>
```

---

### AnyCallNumberNormalizerFilter / LCCallNumberNormalizerFilter / DeweyCallNumberNormalizerFilter

Analysis-chain filters that emit a normalized collation key for a call number token. Because the
entire call number must be treated as a single token, always pair with `KeywordTokenizerFactory`.

- `AnyCallNumberNormalizerFilterFactory` — auto-detects LC vs Dewey; use when scheme is unknown.
- `LCCallNumberNormalizerFilterFactory` — LC only; drops tokens that are not valid LC call numbers
  (unless `echoInvalidInput="true"`).
- `DeweyCallNumberNormalizerFilterFactory` — Dewey only; same drop/pass-through behaviour.

`allowTruncated="true"` accepts partially-specified call numbers (e.g., letters-only prefix
searches); use it in query analyzers when you want to support prefix queries.

**Exact-match / normalized-key search** — both index and query analyzers are identical:

```xml
<fieldType name="callnumber_any" class="solr.TextField">
  <analyzer>
    <tokenizer class="solr.KeywordTokenizerFactory"/>
    <filter class="edu.umich.lib.solr.filter.AnyCallNumberNormalizerFilterFactory"
            allowTruncated="true" echoInvalidInput="true"/>
  </analyzer>
</fieldType>
```

**Prefix / starts-with search** — add `EdgeNGramFilterFactory` at index time only; the query
analyzer just normalizes the prefix query without generating grams:

```xml
<fieldType name="callnumber_starts_with" class="solr.TextField">
  <analyzer type="index">
    <tokenizer class="solr.KeywordTokenizerFactory"/>
    <filter class="edu.umich.lib.solr.filter.AnyCallNumberNormalizerFilterFactory"
            passThroughOnError="false"/>
    <filter class="solr.EdgeNGramFilterFactory" minGramSize="1" maxGramSize="40"/>
  </analyzer>
  <analyzer type="query">
    <tokenizer class="solr.KeywordTokenizerFactory"/>
    <filter class="edu.umich.lib.solr.filter.AnyCallNumberNormalizerFilterFactory"
            passThroughOnError="true" allowTruncated="true"/>
  </analyzer>
</fieldType>
```

---

## Identifier Normalization

### ISBNNormalizerFilter

Normalizes ISBN-10 or ISBN-13 strings (with or without hyphens/spaces) to their canonical
**ISBN-13** form. Tokens that do not look like a valid ISBN are dropped by default; set
`echoInvalidInput="true"` to pass them through unchanged instead.

Use `WhitespaceTokenizerFactory` so a multi-valued field (or a space-separated list) produces one
token per identifier. Because ISBNs are delimited by whitespace in practice, a keyword tokenizer
would work only for single-ISBN fields.

```xml
<fieldType name="isbn" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="edu.umich.lib.solr.filter.ISBNNormalizerFilterFactory"
            echoInvalidInput="true"/>
  </analyzer>
</fieldType>

<field name="isbn" type="isbn" indexed="true" stored="true" multiValued="true"/>
```

---

### LCCNNormalizerFilter

Normalizes Library of Congress Control Numbers to their canonical form (strips leading zeros,
expands two-digit year prefixes, lowercases alphabetic prefixes, etc.). Unlike the ISBN filter,
tokens that do not match the LCCN pattern are always passed through — there is no `echoInvalidInput`
drop behaviour for LCCNs.

```xml
<fieldType name="lccn" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="edu.umich.lib.solr.filter.LCCNNormalizerFilterFactory"/>
  </analyzer>
</fieldType>

<field name="lccn" type="lccn" indexed="true" stored="true" multiValued="true"/>
```

---

## Anchored Search

Both filters add a **numeric position suffix** to every token so that a standard phrase query
becomes position-anchored. They are designed to be dropped into an ordinary `TextField` analysis
chain after tokenization and any normalization you want.

### LeftAnchoredSearchFilter

Appends the 1-based stream position to every term:
`"War and Peace"` → `[War1, and2, Peace3]`

A phrase query `"War and"` matches only documents where those words start at position 1 — i.e.,
the field value *begins with* the query. Use for left-anchored browse (title starts-with, author
starts-with) where you want `"Dickens, C"` to match `"Dickens, Charles"` but not
`"About Dickens, Charles"`.

Use **split analyzers** so the index gets position suffixes but the query analyzer produces the same
suffixed form for matching:

```xml
<fieldType name="title_leftanchored" class="solr.TextField" positionIncrementGap="100">
  <analyzer type="index">
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="edu.umich.lib.solr.filter.LeftAnchoredSearchFilterFactory"/>
  </analyzer>
  <analyzer type="query">
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="edu.umich.lib.solr.filter.LeftAnchoredSearchFilterFactory"/>
  </analyzer>
</fieldType>

<field name="title_left" type="title_leftanchored" indexed="true" stored="true"/>
```

---

### FullyAnchoredSearchFilter

Like `LeftAnchoredSearchFilter` but also appends `"00"` to the **last** token in the stream:
`"War and Peace"` → `[War1, and2, Peace300]`

A phrase query must match both the start (`position 1`) and the special end sentinel (`…00`) to
succeed, so the query must span the *entire* field value. Use for exact-title or exact-authority
searches where partial matches are wrong (e.g., a browse index where `"Dickens, Charles"` must not
match a query for `"Dickens, C"`).

```xml
<fieldType name="title_fullanchored" class="solr.TextField" positionIncrementGap="100">
  <analyzer type="index">
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="edu.umich.lib.solr.filter.FullyAnchoredSearchFilterFactory"/>
  </analyzer>
  <analyzer type="query">
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="edu.umich.lib.solr.filter.FullyAnchoredSearchFilterFactory"/>
  </analyzer>
</fieldType>

<field name="title_full" type="title_fullanchored" indexed="true" stored="true"/>
```

---

## Schema Support

### AnalyzedString

Extends `StrField`; **stores values pre-normalized** through a referenced `TextField`'s index
analysis chain. Unlike `TextField`, `AnalyzedString` stores a single, analysis-chain-normalized
string rather than inverted posting lists — it behaves like a `StrField` for exact-match and range
queries but the stored (and indexed) string has been folded/normalized by your chosen analyzer.

Useful for **browse fields** where you want to store the normalized form for display (e.g., in a
facet or sorted browse list) and still use fast `StrField` semantics for range and sort queries,
without the overhead of a full inverted index.

Configure it by pointing `fieldType` at a companion `TextField` whose **index** analyzer does the
normalization:

```xml
<!-- 1. Define the normalization logic in a plain TextField -->
<fieldType name="text_browse_norm" class="solr.TextField">
  <analyzer type="index">
    <tokenizer class="solr.KeywordTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="solr.ASCIIFoldingFilterFactory"/>
  </analyzer>
  <analyzer type="query">
    <tokenizer class="solr.KeywordTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="solr.ASCIIFoldingFilterFactory"/>
  </analyzer>
</fieldType>

<!-- 2. Point AnalyzedString at that TextField -->
<fieldType name="author_browse_type"
           class="edu.umich.lib.solr.schema.AnalyzedString"
           fieldType="text_browse_norm"/>

<field name="author_browse" type="author_browse_type" indexed="true" stored="true"/>
```
