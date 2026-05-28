# Technical Debt / Deferred Findings

Items deferred from the May 2026 code audit. Ordered roughly by risk/impact.

---

## Testing

### #2 -- Empty test bodies in `AnyCallNumberSimpleTest`

**File:** `src/test/java/edu/umich/lib/normalize/callnumber/AnyCallNumberSimpleTest.java:21-25`

Two test methods (`valid_truncated_key`, `invalid_key`) have empty bodies with no assertions
and no `@Disabled` annotation. They silently pass regardless of behavior, providing false
confidence.

**Action:** Either implement the tests or annotate `@Disabled("TODO: implement")`.

---

### #23 -- `@CsvFileSource(files=...)` uses filesystem-relative paths -- DONE

Resolved: TSV fixtures moved to `src/test/resources/edu/umich/lib/normalize/callnumber/`
and `@CsvFileSource(resources=...)` used in all three test classes.

---

### #24 -- Thin unit test classes could merge with integration tests -- DONE

Resolved: `FullyAnchoredSearchFilterTest` and `LeftAnchoredSearchFilterTest` merged with
their respective `*IntegrationTest` counterparts; the thin files were deleted.

---

### #25 -- ~~Test support classes should be in a dedicated package~~ -- Done

Moved to `edu.umich.lib.solr.testing` package.

---

### #26 -- ~~`TokenStreamTestHelpers` should be deprecated~~ -- Done

Annotated `@Deprecated` with reference to `TokenStreamAsserter`.

---

## Javadoc

### #17 -- `AnyCallNumberSimple` has no Javadoc

**File:** `src/main/java/edu/umich/lib/normalize/callnumber/AnyCallNumberSimple.java`

No class-level or method-level Javadoc.

---

### #18 -- `LCCallNumberSimple` has no class-level Javadoc

**File:** `src/main/java/edu/umich/lib/normalize/callnumber/LCCallNumberSimple.java`

The class has no class-level Javadoc comment.

---

### #20 -- `CallNumberSortKeyFieldType` has no Javadoc

**File:** `src/main/java/edu/umich/lib/solr/schema/CallNumberSortKeyFieldType.java`

No class-level or method-level Javadoc on `toInternal`, `bundledFields`, or `init`.

---

### #21 -- `LCCNNormalizer.normalize()` has no `@return` or `@throws`

**File:** `src/main/java/edu/umich/lib/normalize/lccn/LCCNNormalizer.java:27`

The class-level Javadoc covers the contract, but the method itself has no Javadoc.

---

## Class Design

### #3 -- ~~`AbstractCallNumber.bestKey(Boolean, Boolean)` uses boxed `Boolean` parameters~~ -- Done

Changed to primitive `boolean` parameters.
