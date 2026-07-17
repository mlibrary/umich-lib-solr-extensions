# umich-solr-extensions

Custom Apache Solr/Lucene components developed at the University of Michigan Library. The library provides token filters, field types, and supporting infrastructure for common library-catalog search and indexing patterns: call number normalization and sorting, anchored (left-anchored and fully-anchored) phrase search, and identifier (ISBN/LCCN) normalization.

## Requirements

- Java 17+ (21+ if building against Solr/Lucene 10.x)
- Apache Solr / Lucene 9.x or 10.x

## Components

### Call Number

| Component | Description |
|---|---|
| `CallNumberSortKeyFieldType` | Solr `StrField` subtype that rewrites stored values to call-number collation sort keys via `AnyCallNumberSimple`. Handles both LC and Dewey. Supports `allowTruncated` (default `true`) and `echoInvalidInput` (default `false`). |
| `CallNumberSortableFieldType` | Extends `CallNumberSortKeyFieldType`; stores the **original display value** while indexing with `DOCS`-only options. Use when the field must also be displayed. Skips the field entirely if no usable key exists (and `echoInvalidInput=false`). |
| `AnyCallNumberNormalizerFilter` | Token filter that auto-detects LC vs Dewey call numbers and emits the normalized sort key. Use when call number type is unknown at index time. |
| `LCCallNumberNormalizerFilter` | Like `AnyCallNumberNormalizerFilter`, but restricted to LC call numbers. |
| `DeweyCallNumberNormalizerFilter` | Like `AnyCallNumberNormalizerFilter`, but restricted to Dewey Decimal call numbers. |

### Anchored Search

| Component | Description |
|---|---|
| `LeftAnchoredSearchFilter` | Appends the 1-based stream position to each token (`"the cat"` → `["the1", "cat2"]`). A phrase query only matches when the search string begins at the start of the indexed value. |
| `FullyAnchoredSearchFilter` | Like `LeftAnchoredSearchFilter`, but also appends `"00"` to the final token at index time. Phrase queries must match the field exactly — neither leading nor trailing words are permitted. |

### Identifier Normalization

| Component | Description |
|---|---|
| `ISBNNormalizerFilter` | Canonicalizes ISBN-10 and ISBN-13 tokens to ISBN-13 form. Unrecognizable tokens are dropped unless `echoInvalidInput="true"`. |
| `LCCNNormalizerFilter` | Normalizes Library of Congress Control Numbers. Valid LCCNs are normalized; unrecognizable tokens pass through unchanged. |

### Infrastructure

| Component | Description |
|---|---|
| `SimpleFilter` | Abstract base class for single-token-in / single-token-out transforms. Subclasses implement `munge(String input)`: return the transformed string, or `null` to drop the token (subject to `echoInvalidInput`). |
| `SimpleFilterFactory` | Companion factory base for `SimpleFilter` subclasses. Reads `echoInvalidInput` from schema args and forwards remaining args to the filter constructor. |
| `AnalyzedString` | Solr field type that indexes a value through a named analyzer chain but stores the original. Useful for browse fields where display value and sort/search key differ. |

## Building

The JAR is compiled against a specific Solr/Lucene major version at build time. `solr.version`, `lucene.version`, and `java.version` are Maven properties with defaults for Solr 10 (`solr.version=10.0.0`, `lucene.version=10.3.2`, `java.version=21`); override them on the command line to target a different version:
Maven lets us override any property via -D on the command line regardless of its declared default.

```sh
# Solr 10.x (default) / Java 21
mvn clean package -DskipTests

# Solr 9.x / Java 17
mvn clean package -Dsolr.version=9.7.0 -Dlucene.version=9.11.1 -Djava.version=17 -DskipTests
```

Each build produces a single JAR (`target/umich-solr-extensions-*.jar`) compiled/targeted for the requested version — pick the Solr/Lucene version 
pair that matches the Solr instance you're deploying to. Note that Lucene's major version must match Solr's (Solr 9.x embeds Lucene 9.x, Solr 10.x embeds Lucene 10.x).

The live integration tests (`*LiveIT.java`, run via `mvn verify`, skipped by `-DskipTests`) boot a Solr container via Testcontainers with the built JAR baked in. 
The container image tag tracks `solr.version` automatically (major version only, e.g. `solr:9` or `solr:10`), so `mvn verify -Dsolr.version=9.7.0 ...` tests against a matching Solr 9 container.

## Usage

Build and install the JAR, then reference components in your Solr `schema.xml`. See [Inventory.md](Inventory.md) for detailed descriptions, schema snippets, and configuration options for each component.

## License

See [LICENSE.md](LICENSE.md).
