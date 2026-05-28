# umich-lib-solr-extensions

Custom Apache Solr/Lucene components developed at the University of Michigan Library. The library provides token filters, field types, and supporting infrastructure for common library-catalog search and indexing patterns: call number normalization and sorting, anchored (left-anchored and fully-anchored) phrase search, and identifier (ISBN/LCCN) normalization.

## Requirements

- Java 21+
- Apache Solr / Lucene 10.x

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

## Usage

Build and install the JAR, then reference components in your Solr `schema.xml`. See [docs/components.md](docs/components.md) for detailed descriptions, schema snippets, and configuration options for each component.

To build a new filter using the `SimpleFilter` / `SimpleFilterFactory` scaffolding, see [docs/simple-filter.md](docs/simple-filter.md).

Example schema fragments are in [docs/examples/schema/](docs/examples/schema/).

## License

See [LICENSE.md](LICENSE.md).
