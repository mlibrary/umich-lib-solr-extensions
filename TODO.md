# Technical Debt / Deferred Findings

Items deferred from the May 2026 code audit. Ordered roughly by risk/impact.

---

## Testing

### #2 — Empty test bodies in `AnyCallNumberSimpleTest`

**File:** `src/test/java/edu/umich/lib/normalize/callnumber/AnyCallNumberSimpleTest.java:21-25`

Two test methods (`valid_truncated_key`, `invalid_key`) have empty bodies with no assertions
and no `@Disabled` annotation. They silently pass regardless of behavior, providing false
confidence.

**Action:** Either implement the tests or annotate `@Disabled("TODO: implement")`.

---

### #23 — `@CsvFileSource(files=...)` uses filesystem-relative paths

**Files:**
- `AnyCallNumberSimpleTest.java:12`
- `DeweySimpleTest.java:11`
- `LCCallNumberSimpleTest.java:9`

Uses `@CsvFileSource(files = "src/test/java/...")` which binds tests to the working
directory. Tests break when run outside the project root.

**Action:** Switch to `@CsvFileSource(resources = "/path/from/classpath")` and move the
TSV fixtures to `src/test/resources/`.

---

### #24 — Thin unit test classes could merge with integration tests

**Files:**
- `FullyAnchoredSearchFilterTest.java` — one test (`testNested`) that duplicates coverage
  already present in `FullyAnchoredSearchFilterIntegrationTest`.
- `LeftAnchoredSearchFilterTest.java` — two tests (`testOneToken`, `testNested`) likewise.

**Action:** Absorb these into the corresponding `IntegrationTest` classes, then delete the
thin files.

---

### #25 — ~~Test support classes should be in a dedicated package~~ ✓ Done

Moved to `edu.umich.lib.solr.testing` package.

---

### #26 — ~~`TokenStreamTestHelpers` should be deprecated~~ ✓ Done

Annotated `@Deprecated` with reference to `TokenStreamAsserter`.

---

## Javadoc

### #17 — `AnyCallNumberSimple` has no Javadoc

**File:** `src/main/java/edu/umich/lib/normalize/callnumber/AnyCallNumberSimple.java`

No class-level or method-level Javadoc.

---

### #18 — `LCCallNumberSimple` has no class-level Javadoc

**File:** `src/main/java/edu/umich/lib/normalize/callnumber/LCCallNumberSimple.java`

The class has no class-level Javadoc comment.

---

### #20 — `CallNumberSortKeyFieldType` has no Javadoc

**File:** `src/main/java/edu/umich/lib/solr/schema/CallNumberSortKeyFieldType.java`

No class-level or method-level Javadoc on `toInternal`, `bundledFields`, or `init`.

---

### #21 — `LCCNNormalizer.normalize()` has no `@return` or `@throws`

**File:** `src/main/java/edu/umich/lib/normalize/lccn/LCCNNormalizer.java:27`

The class-level Javadoc covers the contract, but the method itself has no Javadoc.

---

## Class Design

### #3 — `AbstractCallNumber.bestKey(Boolean, Boolean)` uses boxed `Boolean` parameters

**File:** `src/main/java/edu/umich/lib/normalize/callnumber/AbstractCallNumber.java:79`

Parameters are `Boolean` (boxed) rather than `boolean` (primitive). Boxed booleans can be
`null`, causing `NullPointerException` on auto-unboxing.

**Action:** Change parameter types to primitive `boolean`.
