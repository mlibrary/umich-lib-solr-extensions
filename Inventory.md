# Filter Inventory

All filters are Lucene `TokenFilter` / `TokenFilterFactory` pairs operating at analysis time.

## Infrastructure

| Class | Use Case |
|-------|----------|
| `SimpleFilter` | Abstract base for 1-in/1-out token transforms; subclasses implement `munge(String)`. Supports `echoInvalidInput` to pass bad tokens through instead of dropping them. |
| `SimpleFilterFactory` | Companion factory base; reads `echoInvalidInput` from schema args and forwards all args to the filter constructor. |

## Call Number Normalization

| Class | Use Case |
|-------|----------|
| `LCCallNumberNormalizerFilter` | Normalizes LC Call Numbers to a sortable key for sort fields and left-anchored prefix search. |
| `DeweyCallNumberNormalizerFilter` | Same as above for Dewey Decimal call numbers. |
| `AnyCallNumberNormalizerFilter` | Auto-detects LC vs Dewey and normalizes accordingly — use when call number type is unknown at index time. |
| `*Factory` (×3) | Schema wiring for the three call number filters above. |

## Identifier Normalization

| Class | Use Case |
|-------|----------|
| `ISBNNormalizerFilter` | Canonicalizes ISBNs to ISBN-13; drops unrecognizable tokens (or passes through if `echoInvalidInput="true"`). |
| `LCCNNormalizerFilter` | Normalizes LCCNs; always passes tokens through (normalized if valid, unchanged if not). |
| `*Factory` (×2) | Schema wiring for the two identifier filters above. |

## Anchored Search

| Class | Use Case |
|-------|----------|
| `LeftAnchoredSearchFilter` | Appends stream position as a suffix (`token` → `token1`) so phrase queries only match when the query starts at the beginning of the field. |
| `FullyAnchoredSearchFilter` | Buffers the full token stream, adds position suffixes, and appends `"00"` to final-position tokens — phrase queries must match the field exactly (no leading or trailing words). |
| `*Factory` (×2) | Schema wiring for the two anchored search filters above. |
