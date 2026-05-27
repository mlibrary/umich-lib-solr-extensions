# umich-lib-solr-extensions — Reorganization Plan

(repo and artifact rename from `umich-solr-extensions` — see §0)

Opinionated audit. Severity tags: **[BLOCKER]** ships broken or near-broken, **[SHARP]** lurking footgun, **[HYGIENE]** taste / future-maintainer cost. Items are listed in the order you should attack them.

---

## Breaking Changes for Existing Users

This reorg introduces intentional breaking changes. Downstream deployments **must** update before upgrading to any release cut from this work.

### B.1 `schema.xml` — filter attribute rename

All filters that previously accepted `passThroughOnError` now require `echoInvalidInput`. This rename has already landed in the source; it is not negotiable. Update every `schema.xml` that configures one of these factories:

- `AnyCallNumberNormalizerFilterFactory`
- `LCCallNumberNormalizerFilterFactory`
- `DeweyCallNumberNormalizerFilterFactory`
- `ISBNNormalizerFilterFactory`
- `LCCNNormalizerFilterFactory`

**Before:**
```xml
<filter class="edu.umich.lib.solr.filter.ISBNNormalizerFilterFactory"
        passThroughOnError="true"/>
```
**After:**
```xml
<filter class="edu.umich.lib.solr.filter.ISBNNormalizerFilterFactory"
        echoInvalidInput="true"/>
```

Schemas that use the old attribute name will silently ignore it once §4.8 is fixed (Lucene's unknown-args validation is re-enabled). Before §4.8 lands, they silently misbehave. Either way the old name no longer does anything.

### B.2 `SimpleFilter` subclasses — API changes

If you extend `SimpleFilter` outside this repo, the following changes require code updates (§4.1):

| What changed | Action required |
|---|---|
| `SimpleFilter` is now `abstract` | No change if you were already providing `munge()` |
| `munge(String)` is now `abstract` — no default pass-through | Implement `munge()` if you weren't already |
| `SimpleFilter(TokenStream)` and `SimpleFilter(TokenStream, boolean)` constructors removed | Switch to the `(Map<String,String>)` constructor path via `SimpleFilterFactory` |
| `SimpleFilterFactory` is now `abstract` | No change if you were already providing a concrete subclass |
| `SimpleFilterFactory` no-arg constructor removed | No action needed (it only existed to throw) |

Java package names are **not** changing. No `schema.xml` `class="..."` references need updating for this reason alone.

---

## 0. Rename: `umich-solr-extensions` → `umich-lib-solr-extensions`

Do this **after** the §1 BLOCKERs land (broken artifact bugs should not survive longer than necessary, even for a one-PR delay), but before the rest of the restructuring — so subsequent PRs land under the final name and history search doesn't have to bridge a rename mid-stream. Execution order in §7 reflects this.


### 0.1 GitHub repo rename

- Rename the repo via GitHub UI (Settings → Repository name). GitHub auto-installs a permanent redirect for the old URL, so existing clones, issue links, and `git remote` URLs keep working — but update them anyway:
  - `git remote set-url origin git@github.com:<org>/umich-lib-solr-extensions.git` on every active clone.
  - Update any submodule references, CI cross-repo triggers, and badge URLs in `README.md`.
- Update branch protection / CODEOWNERS / required-checks references that hardcode the repo name.

### 0.2 Maven coordinates

Change in `pom.xml`:

```xml
<artifactId>umich-lib-solr-extensions</artifactId>
```

`groupId` stays `edu.umich.lib` — it already reflects the org. When §2.2 splits into modules, the parent becomes `umich-lib-solr-extensions-parent` (packaging=pom) with children `normalize` and `solr-extensions`; the deployable plugin JAR keeps `umich-lib-solr-extensions` as its artifactId so downstream consumers don't have to chase a second rename.

**Breaking for any existing consumer — but no consumer exists yet.** `version 1.0` on trunk has never been published (the POM is misconfigured per §1.5; nothing in this repo deploys). Cut over to the new coordinate directly; do not publish a `<relocation>` POM. (If a real downstream consumer surfaces during the rename PR, publish a final release of the old coordinate with a `<relocation>` pointing at the **released** version of the new coordinate — never at a `-SNAPSHOT`.)


### 0.3 In-repo references

Replace the enumerated list with a literal command — enumeration is guaranteed to drift:

```sh
git grep -l umich-solr-extensions -- ':!.git'
```

Audit every hit. Known categories the command will surface:

- `pom.xml` — `<artifactId>`, `<name>`
- `docker/docker-compose.yml` — `container_name: umich-solr-ext` (rename to `umich-lib-solr-ext`), volume mount path `/opt/umich-ext` → `/opt/umich-lib-ext`
- `docker/init-libs.sh` — `SRC_DIR`, JAR glob, stale-copy `rm -f` pattern (both globs must match the new artifactId or the rebuild loop silently keeps old jars around)
- `AbstractLiveIT` — `findProjectJar` JAR-name pattern
- `src/test/resources/solr-integration/test-core/conf/{solrconfig,schema}.xml` — verify no comment or `<lib>` directive references the old name
- `.github/workflows/*.yml` — CI references
- `README.md`, `Inventory.md`, `README_SimpleFilter.md`, `TODO.md`, `ReorgPlan.md` — title, install snippets, path examples
- `.idea/modules.xml`, `.idea/runConfigurations/*`, `umich-solr-extensions.iml` — IntelliJ module name (if kept; see §5.1, these should be ignored not tracked)
- `out/` — delete; it contains compiled output under the old name


### 0.4 Java package names — **DO NOT rename**

Packages stay `edu.umich.lib.normalize.*` and `edu.umich.lib.solr.*`. Renaming Java packages is a breaking change for every `schema.xml` in production that references `class="edu.umich.lib.solr.filter.…"`. The artifact rename does not require a package rename, and conflating the two would force every downstream Solr deployment to edit their schemas.

### 0.5 Verification checklist before merging the rename PR

- [ ] `mvn clean verify` green
- [ ] `mvn package && docker compose -f docker/docker-compose.yml up` brings up Solr and the JAR loads (check Solr admin UI → Plugins → analysis filter factories list the `edu.umich.lib.solr.filter.*` entries)
- [ ] `grep -r umich-solr-extensions .` returns zero hits **outside** `.git/`, `docs/changelog/`, and any explicitly historical reference
- [ ] CI workflow runs green on the renamed repo
- [ ] Old repo URL still redirects (GitHub does this automatically; confirm)

---

## 1. Build / Maven

### 1.1 [BLOCKER] Solr/Lucene are bundled into the plugin JAR

`pom.xml` declares `lucene-core`, `lucene-analysis-common`, `lucene-queries`, `solr-core`, `solr-solrj` at **default `compile` scope**. A Solr plugin JAR that drops into `${SOLR_HOME}/lib` must NEVER ship its own copy of Solr/Lucene — at best you waste 80+ MB, at worst you get `LinkageError`s when the plugin's `TokenFilter` subclass is loaded by a different classloader than the host's `TokenFilter`. Make every Solr/Lucene/SLF4J production dep (including `slf4j-api`) `<scope>provided</scope>`. The init-libs.sh / Testcontainers wiring already assumes Solr provides them; the POM has just been silently lying.

Before blanket-pinning, run `mvn dependency:analyze` and trim: most filter classes need only `lucene-core` + `lucene-analysis-common`; `solr-core` is touched only by the `schema/*FieldType` classes. After §2.2's module split, the `normalize` module needs zero Solr/Lucene deps. `lucene-queries` is not currently imported anywhere — verify and drop it if so.


### 1.2 [BLOCKER] `testcontainers` version `2.0.5` does not exist

Testcontainers ships `1.x` (current `1.20.x`). `2.0.5` either resolves to something unintended or breaks transitive resolution. Pin to a real release (`1.20.4` as of this writing) and use the BOM (`org.testcontainers:testcontainers-bom`) so the container modules stay in lockstep.

### 1.3 [BLOCKER] `SimpleFilterFactory` is registered in `META-INF/services/...TokenFilterFactory`

`SimpleFilterFactory`'s public no-arg ctor *throws* `UnsupportedOperationException`. It is a scaffold base class, not a usable SPI entry. Lucene's `AnalysisSPILoader` instantiates entries via the no-arg ctor at discovery; registering it will at minimum spam logs and at worst poison SPI loading depending on Lucene version. Remove the line. Same for `UpcaseWordsThatStartWithFilterFactory` — example code does not belong in the production SPI manifest.

### 1.4 [SHARP] Hardcoded versions, no `dependencyManagement`, no BOM

Use the Solr BOM (`org.apache.solr:solr-bom`) in `<dependencyManagement>` and declare Lucene deps without `<version>` — solr-bom pins the matching Lucene line, which kills the current `lucene 10.3.2` / `solr 10.0.0` mismatch. (Lucene does not publish its own BOM — `org.apache.lucene:lucene-bom` does not exist on Maven Central. The historical `org.apache.solr:solr-parent` is also not a BOM in the modern sense; use `solr-bom`.)


### 1.5 [SHARP] `version 1.0` is not a SNAPSHOT

Trunk should be `1.0.0-SNAPSHOT` (or `0.x.y-SNAPSHOT`). `1.0` as a release version on `main` means every CI build overwrites the same coordinate; downstream consumers cache it forever.

### 1.6 [SHARP] No `maven-enforcer-plugin`

Add enforcer rules: `requireJavaVersion`, `requireMavenVersion`, `bannedDependencies` (ban `commons-logging`, `log4j:log4j`, `slf4j-log4j12`, `slf4j-jdk14` so a stray transitive doesn't conflict with Solr's logging), and `requireReleaseDeps` scoped to the release profile only (no SNAPSHOT deps in a release).

Do **not** add `dependencyConvergence` — Solr's transitive graph carries dozens of legitimate version conflicts. Rely on `dependency:analyze` (§5.3) plus the Solr BOM to catch real version drift. Adding the rule would require permanent suppressions that are harder to audit than the underlying problem.


### 1.7 [SHARP] No `Automatic-Module-Name` / no manifest customization

Solr 10 on Java 21 — set `<Automatic-Module-Name>edu.umich.lib.solr.extensions</Automatic-Module-Name>` in the JAR manifest. Cheap hygiene; no JPMS roadmap is implied (Solr itself is not modularized). Also add `Implementation-Version`, `Implementation-Vendor`, `Implementation-Title`, and the Git SHA (via `git-commit-id-maven-plugin`). Field reports against unknown builds become tractable.


### 1.8 [HYGIENE] No Spotless / Checkstyle / Error Prone / SpotBugs

Pick one formatter (Spotless with `palantir-java-format` or Google) and one static analyzer (Error Prone is cheapest in CI) and wire them into `verify`. Right now formatting drift is inevitable and pre-merge.

### 1.9 [HYGIENE] No `maven-source-plugin` / `maven-javadoc-plugin`

Attach `*-sources.jar` and `*-javadoc.jar` at `package` via `maven-source-plugin` and `maven-javadoc-plugin`. Required for publishing.

Target: **GitHub Packages**. POM additions:

- `<scm>` block pointing at `https://github.com/<org>/umich-lib-solr-extensions.git` (required by some downstream tooling, cheap to add).
- `<licenses>` block matching `LICENSE.md`.
- `<distributionManagement>` pointing at `https://maven.pkg.github.com/<org>/umich-lib-solr-extensions`.
- No GPG signing (not required by GitHub Packages).
- `<developers>` block: optional for GitHub Packages, recommended.

Auth: consumers use `GITHUB_TOKEN` (CI) or a PAT with `read:packages` (local) configured in `~/.m2/settings.xml` against the `github` server id.

Also: add SPDX or copyright headers to every Java source file. Wire `license-maven-plugin` (or Spotless's `licenseHeader` step from §1.8) to enforce on `verify`.


### 1.10 [HYGIENE] No release pipeline

Tag-driven release via GitHub Actions:

- Push tag `vX.Y.Z` → workflow runs `mvn versions:set -DnewVersion=X.Y.Z`, `mvn deploy -P release`, `gh release create`.
- The `release` profile activates the source/javadoc plugins (§1.9), the `requireReleaseDeps` enforcer rule (§1.6), and the GitHub Packages `<distributionManagement>`.
- `main` stays on `X.Y.Z-SNAPSHOT` (§1.5); the workflow bumps to the next snapshot after release.

Do not use `maven-release-plugin` — its two-commit dance plays poorly with branch protection and adds nothing over the tag-driven flow.

### 1.11 [HYGIENE] Legacy Surefire `argLine`

Both Surefire and Failsafe set `-Djava.security.egd=file:/dev/./urandom`. That was a Java 7-era workaround for slow `/dev/random` on Linux; irrelevant on Java 21. Delete from both plugin configs.


---

## 2. Source layout

### 2.1 [BLOCKER] Example code lives in `src/main`

`UpcaseWordsThatStartWithFilter` / `…Factory` are pedagogical examples for `README_SimpleFilter.md`. They have no place in the published artifact. Move them to one of:
- `src/test/java/...` if they are only used by `UpcaseWordsThatStartWithFilterTest`, **OR**
- a separate Maven module `examples/` not attached to the main JAR.

Then remove `UpcaseWordsThatStartWithFilterFactory` from `META-INF/services/...`. **Land this with §1.3 and §4.1 as a single PR** — the SPI line, the class location, and the abstract-class status are coupled; splitting them invites someone to re-add the bad SPI entry "because it was missing."


### 2.2 [SHARP] Two-axis package split is half-implemented

You have `edu.umich.lib.normalize.*` (pure Java normalizers) and `edu.umich.lib.solr.*` (Solr/Lucene adapters). Good instinct. Finish it: split into two Maven modules.

```
umich-solr-extensions/                <-- parent pom, packaging=pom
  normalize/                          <-- pure java, no Solr deps, junit only
    edu.umich.lib.normalize.callnumber
    edu.umich.lib.normalize.isbn
    edu.umich.lib.normalize.lccn
  solr-extensions/                    <-- the deployable plugin JAR
    edu.umich.lib.solr.filter
    edu.umich.lib.solr.schema
    depends on :normalize
```

Benefits, in order of importance:

1. **Compile-time isolation.** `normalize` cannot accidentally `import org.apache.lucene.*` if Lucene is not on its classpath. This is the only durable way to keep the layering honest.
2. **Test speed.** `normalize` tests don't pay the Solr/Lucene classpath tax.
3. **Independent versioning later**, if a non-Solr consumer ever materializes. Do not justify the split on hypothetical reuse today.


### 2.3 [SHARP] Test resources live under `src/test/java`

`any_valid_key_verification.tsv`, `dewey_verification.tsv`, `lc__verification.tsv` are in `src/test/java/edu/umich/lib/normalize/callnumber/`. Maven treats `src/test/java` as a source root; non-`.java` files there are nominally copied but are not part of `test-classpath` in the predictable way `src/test/resources` is. This is TODO #23 in `TODO.md` — promote it from "deferred" to "now". Move all fixture files to `src/test/resources/fixtures/callnumber/` and switch `@CsvFileSource(files=...)` to `@CsvFileSource(resources=...)`.

### 2.4 [SHARP] Integration test naming is inconsistent and breaks Failsafe split

Surefire's default includes are `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java` — `*IntegrationTest.java` matches the `*Test.java` rule and runs in the **unit** phase, which is the bug. Failsafe's default include is `**/*IT.java`.

- `FullyAnchoredSearchFilterIntegrationTest.java` — runs in Surefire because it ends in "Test", not because anyone configured it to.
- `LeftAnchoredSearchFilterIntegrationTest.java` — same.
- `LccnNormalizerLiveIT.java`, `IsbnNormalizerLiveIT.java`, etc. — `*IT.java`, correctly go to Failsafe.

Pick one convention. Recommended: `*Test.java` = unit (Surefire), `*IT.java` = live/integration (Failsafe). Rename `*IntegrationTest.java` files — they are not actually integration tests in the Solr sense, they exercise the filter through an in-process `TokenStream`. Call them `…FilterTest` and merge with the existing thin test files per TODO #24.

Then **make the convention explicit in the build**: set `<includes>` and `<excludes>` on both Surefire and Failsafe with the chosen patterns. Plugin defaults drift across versions; explicit includes don't.


### 2.5 [HYGIENE] `sample_schema/` at repo root duplicates the test configset

`sample_schema/callnumber_browse.xml` and `authority_browse.xml` are documentation. Move to `docs/examples/schema/` and add a one-line README pointing at them from the main README. While you're at it, verify they actually validate against the test-core's `solrconfig.xml` — sample code that has drifted is worse than no sample code.

### 2.6 [HYGIENE] Markdown sprawl in repo root

`README.md`, `README_SimpleFilter.md`, `Inventory.md`, `TODO.md`, `LICENSE.md`. Move everything that isn't `README.md` or `LICENSE.md` into `docs/`:
- `docs/components.md` ← `Inventory.md`
- `docs/simple-filter.md` ← `README_SimpleFilter.md`
- `docs/tech-debt.md` ← `TODO.md` (or convert to GitHub issues and delete)

Cross-link from `README.md`. Repo root should be ~5 files, not a documentation dumping ground.

---

## 3. Solr integration / runtime

### 3.1 [SHARP] Two parallel Solr-on-Docker setups

`docker/docker-compose.yml` + `init-libs.sh` is one path; `AbstractLiveIT` (Testcontainers) is another. They use **different mount strategies** (compose: bind-mount JAR to `${SOLR_HOME}/lib` via init script; Testcontainers: bake into an `ImageFromDockerfile` at `/opt/solr/lib/`), **different configset paths** (`/opt/configs/test-core` vs `withCopyFileToContainer`), and they will rot independently.

Pick one and make it canonical:
- Testcontainers is the right choice for tests (already wired, hermetic, no global port collision).
- Keep `docker-compose.yml` only if humans actually use it for manual exploration; otherwise delete and document `mvn -Plive-it failsafe:integration-test`.

If you keep both, share the configset path constant and the JAR-discovery logic via a single helper used by both `init-libs.sh` (via env) and `AbstractLiveIT`.

### 3.2 [SHARP] `AbstractLiveIT` builds an image on every JVM start

`ImageFromDockerfile` is slow and clutters the local Docker cache. Use the `solr:10` image directly and `withCopyFileToContainer(MountableFile.forHostPath(jar), SOLR_LIB_PATH + "/" + jar.getName())`. Same effect, no Docker build, no cache pollution.

The mount destination must be `${SOLR_HOME}/lib` (typically `/var/solr/data/lib` in the stock `solr:10` image) — **not** `/opt/solr/lib/` as the current `ImageFromDockerfile` uses, and not whatever path looks plausible. `docker/init-libs.sh` already uses `${SOLR_HOME}/lib`; the Testcontainers wiring must mirror it. Factor the path into a single constant (e.g. `static final String SOLR_LIB_PATH = "/var/solr/data/lib";`) referenced from both sites.


### 3.3 [SHARP] Live-IT skip semantics conflate "JAR missing" with "skip"

`AbstractLiveIT.initSolrContainer()` returns null when `target/` or the JAR isn't built. That is silently treated identically to "Docker absent / explicit skip flag", which conflates a real bug with intentional opt-out.

Fix in two parts:

1. **Explicit skip knob.** Add a system property `-DskipLiveITs=true` (or a Maven profile `-P live-it` that *enables* the live ITs, with default = disabled). The base class checks the knob first.
2. **No knob → JAR missing is a hard failure.** Failsafe runs after `package`; absence of the JAR is a build bug. Throw, do not skip. Once §2.4 lands, the live-IT classes never run in Surefire, so the Surefire path is moot.


### 3.4 [SHARP] `solrconfig.xml` for the test-core needs an `Inventory.md` snippet pinning what `solr-precreate` actually exposes

The test configset is exercised by every live IT but its capabilities are nowhere documented. Add a header comment in `solrconfig.xml` listing which field types & request handlers are configured, and why each exists.

### 3.5 [HYGIENE] `init-libs.sh` lives in `/docker-entrypoint-initdb.d`

That directory is a Solr-image convention you're leaning on without documentation. Add a comment in `init-libs.sh` linking to the Solr Docker image docs that establish this contract — the next maintainer should not have to spelunk Docker Hub.

---

## 4. Code-level

### 4.1 [SHARP] `SimpleFilter` is not abstract

`public class SimpleFilter` has a default `munge` that returns input unchanged. That makes the class instantiable as a no-op filter and weakens the contract — subclasses might forget to override and silently pass everything through. Make it `abstract` and remove the default `munge`.

Same for `SimpleFilterFactory` — mark `abstract` and delete the public no-arg ctor (which currently exists only to throw `UnsupportedOperationException`). Once abstract, `AnalysisSPILoader` will physically refuse to instantiate it, which is a stronger guarantee than just removing the SPI manifest line.

Also delete the convenience constructors `SimpleFilter(TokenStream)` and `SimpleFilter(TokenStream, boolean)` — every real subclass passes the args map, and a no-arg-style constructor on an abstract scaffold is dead surface.

`incrementToken()` is already `final`; tighten the class Javadoc so `munge` is identified as the sole extension point. **Land this with §1.3 and §2.1 as a single PR** — the SPI cleanup and the abstract conversion reinforce each other.


### 4.2 [SHARP] `SimpleFilterFactory.normalizeArgs` is dead code

`normalizeArgs` existed to translate the legacy `passThroughOnError` attribute to `echoInvalidInput`. That rename has already shipped; all in-repo factories now use `echoInvalidInput` directly. Verify with `git grep normalizeArgs` — if no factory calls it, delete it. Delete the corresponding documentation in `README_SimpleFilter.md` / `docs/simple-filter.md` at the same time.

### 4.3 [SHARP] `SimpleFilter` field name `myTermAttribute`

`my*` prefix in production code is a smell. Rename to `termAttr`. Likewise `filterArgs` is fine.

### 4.4 [SHARP] `SimpleFilter.incrementToken()` skips `munge` for empty tokens but still returns `true`

Read the code carefully: when `t.isEmpty()` is true, `incrementToken` does **not** call `setEmpty()` and does **not** call `munge`; the upstream `CharTermAttribute` is left untouched and the method returns `true`. Net effect: empty upstream tokens propagate unchanged, and `munge` never sees them. Two cleanups:

- The `t == null` branch above the empty check is dead — `CharTermAttribute.toString()` never returns `null`. Delete it.
- Decide whether bypassing `munge` for empty tokens is intentional. If yes, document it in the Javadoc ("empty upstream tokens are emitted unchanged without calling `munge`"). If no, fold the empty case into the normal `munge` path. Do not defer this — a future change to "drop empties too" would be a stealth breaking change either way.


### 4.5 [HYGIENE] Missing Javadoc on public surface (TODO #17/18/20/21)

Pull these out of `TODO.md` and just do them as part of this reorg. Public Solr-plugin API without Javadoc is hostile to downstream integrators who configure your filters in `schema.xml` and have no IDE help when something's wrong.

### 4.6 [HYGIENE] `TokenStreamTestHelpers` is deprecated in place

Delete it. Deprecation in a closed-source private test package buys nothing — there are no external callers. The TODO says it's been replaced by `TokenStreamAsserter`; finish the migration and remove the file.

### 4.7 [HYGIENE] Empty-bodied tests in `AnyCallNumberSimpleTest`

TODO #2. Implement or `@Disabled("reason")`. A green test that asserts nothing is worse than a missing test.

### 4.8 [SHARP] `SimpleFilterFactory` silently disables Lucene's unknown-args validation

Lucene's `TokenFilterFactory` constructor expects to be handed a *mutable* args map from which it `remove()`s recognized keys; whatever remains is reported as an "unknown parameter" error. `SimpleFilterFactory` passes `new HashMap<>(args)` to `super(args)` and keeps its own immutable snapshot. Net effect: every `schema.xml` typo (e.g. `echoInvlaidInput="true"`) is silently ignored — the framework's free validation is disabled.

Fix: snapshot first, then pass the **original** mutable map to `super(args)` so the framework consumes it. Verify against the pinned Lucene version that the framework still reports leftovers (the API has historically been `assertNoUnknownArgs` or similar). Add a unit test that constructs a factory with a bogus arg and asserts it throws.

### 4.9 [SHARP] No SPI smoke test

Nothing currently verifies that every entry in `META-INF/services/org.apache.lucene.analysis.TokenFilterFactory` can actually be loaded by `AnalysisSPILoader` with a representative args map. Such a test would have caught §1.3 the moment the bad line was added.

Add `src/test/java/edu/umich/lib/solr/spi/ServiceLoaderRegistrationTest.java`: one test, iterates the manifest, instantiates each entry with `Map.of()` (and again with a known-bad arg to exercise §4.8), asserts non-null and that `create(TokenStream)` returns a non-null `TokenFilter`. Cheap insurance against future SPI regressions.

### 4.10 `AnalyzedString` — placement and status

`AnalyzedString` is a production `FieldType` that extends Solr's `StrField`. It stores the value pre-normalized through a named `TextField`'s analyzer chain — giving browse/facet fields fast `StrField` storage semantics with controlled normalization at index time.

**Where it lives:** `solr-extensions` module, `edu.umich.lib.solr.schema` package, alongside `CallNumberSortableFieldType` and `CallNumberSortKeyFieldType`. It has no dependency on `normalize/` — it delegates all analysis work to the named Solr field's analyzer chain and is otherwise self-contained.

**What the plan must ensure for `AnalyzedString`:**

- It is documented in `docs/components.md` (the renamed `Inventory.md`) with a schema usage example, including the required `analyzerField` attribute.
- `sample_schema/authority_browse.xml` (moving to `docs/examples/schema/`) demonstrates its use; verify the example still works against the post-reorg test configset.
- It is covered by at least one live IT that indexes a document through the field type and retrieves it — confirming the normalizer delegation survives the Solr boot sequence. Add this test in the same PR as the other live-IT consolidation work (§3.1 / §3.2).
- Its `echoInvalidInput` behavior (or absence thereof — `AnalyzedString` operates at schema level, not analysis chain level) is documented. Clarify whether it propagates or suppresses normalization errors, and what the failure mode is for an `analyzerField` that doesn't exist at boot time.


---

## 5. Repo hygiene

### 5.1 [SHARP] IDE config checked in

`.classpath`, `.project`, `.settings/`, `.factorypath`, `umich-solr-extensions.iml` are in the working tree (visible in mtime listing). `.gitignore` lists them — verify they aren't tracked, and if they are, `git rm --cached` them. Same for `out/` (IntelliJ output dir).

### 5.2 [SHARP] Agent/tooling state in repo

`.brv/`, `.yantrik/`, `.agentbridge/`, `.agent-work/`, `.hindsight.yaml`, `.opencode/` (and the `opencode.jsonc` file). At least `.brv/` and `.yantrik/` are ignored. Audit the rest. `opencode.jsonc` at repo root is fine if it's project-wide policy; everything else is per-user state and should be ignored or moved into `~/.config/...`.

### 5.3 [HYGIENE] CI runs only `mvn verify` on one JDK

- Add a matrix: Java 21 (Solr 10's minimum). Add Java 25 only after confirming Solr's own CI tests against it — pin to Temurin builds Solr has actually exercised; do not chase whatever LTS happens to be newest. Solr 10's published system requirements (May 2026) state "Java 21 or greater" with no upper bound declared.
- Add a job that runs `mvn -P live-it verify` with Docker available (Ubuntu runners have it) so the live ITs actually run in CI, not just locally.
- Cache the Testcontainers image layer.
- Run `mvn dependency:analyze` and fail on unused-declared and used-undeclared.


### 5.4 [HYGIENE] No `CONTRIBUTING.md`, no `CODEOWNERS`, no PR template

Optional but cheap. With Solr conventions in play (SPI registration, scope rules, configset compatibility), a `CONTRIBUTING.md` that says "if you add a TokenFilterFactory, add it to `META-INF/services/...`; if you add a new field type, document it in `docs/components.md`; provided-scope is mandatory for solr/lucene deps" is worth its weight.

---

## 6. Proposed final layout

```
umich-lib-solr-extensions/
├── README.md
├── LICENSE.md
├── CONTRIBUTING.md
├── pom.xml                            (parent, packaging=pom)
├── .github/workflows/ci.yml           (matrix + live-IT job)
├── docs/
│   ├── components.md                  (was Inventory.md; covers all FieldTypes incl. AnalyzedString)
│   ├── simple-filter.md               (was README_SimpleFilter.md)
│   ├── migrating.md                   (breaking changes; see §B)
│   ├── tech-debt.md                   (was TODO.md, or converted to GH issues)
│   └── examples/schema/
│       ├── callnumber_browse.xml
│       └── authority_browse.xml       (demonstrates AnalyzedString usage)
├── docker/                            (optional: only if manual dev loop is real)
│   ├── docker-compose.yml
│   └── init-libs.sh
├── normalize/                         (module: pure java, no Solr/Lucene deps)
│   ├── pom.xml
│   └── src/{main,test}/{java,resources}
│       └── edu/umich/lib/normalize/{callnumber,isbn,lccn}
├── solr-extensions/                   (module: the deployable plugin JAR)
│   ├── pom.xml                        (solr/lucene = provided)
│   └── src/
│       ├── main/
│       │   ├── java/edu/umich/lib/solr/{filter,schema}
│       │   │   └── schema/AnalyzedString.java  (StrField subtype, self-contained)
│       │   └── resources/META-INF/services/
│       │       └── org.apache.lucene.analysis.TokenFilterFactory
│       └── test/
│           ├── java/edu/umich/lib/solr/{filter,schema,spi,testing,integration/live}
│           └── resources/
│               ├── fixtures/callnumber/*.tsv         (moved from src/test/java)
│               ├── fixtures/integration/*.jsonl
│               └── solr-integration/{solr.xml, test-core/conf/*}
└── examples/                          (optional module, not published)
    └── upcase-words-that-start-with/  (the simple-filter.md example)
```

---

## 7. Execution order

Do them in this order so each step compiles & tests pass:

1. **Fix the POM BLOCKERs** — provided scopes (1.1), Testcontainers version (1.2), solr-bom (1.4), SNAPSHOT (1.5), delete legacy `argLine` (1.11). Single PR; the artifact is broken right now and should not stay broken through a multi-PR rename.
2. **Fix the SPI manifest + make `SimpleFilter`/`SimpleFilterFactory` abstract + move `UpcaseWordsThatStartWithFilter*` to test scope** — §1.3, §4.1, §2.1 as a single coupled PR. Also fix the unused-args contract (§4.8) and add the SPI smoke test (§4.9) here so the regression is locked down before any other code moves.
3. **Rename** repo + artifact to `umich-lib-solr-extensions` (§0). Standalone PR, high blast-radius.
4. **Move fixtures** — `src/test/java/**/*.tsv` → `src/test/resources/fixtures/callnumber/` (2.3).
5. **Rename integration tests** to `*IT` or `*Test` correctly, merge thin tests, pin Surefire/Failsafe includes explicitly (2.4 / TODO #24).
6. **Move docs and samples** into `docs/` (2.5, 2.6).
7. **Modularize** — split `normalize` into its own Maven module (2.2). Largest mechanical refactor by far (every file moves, two new POMs, parent POM rewrite); will substantially exceed any per-PR line cap. Land as its own PR after everything above stabilizes.
8. **CI matrix + live-IT job** (5.3).
9. **Formatter + enforcer + analyzer + license headers** (1.6, 1.8, 1.9 headers).
10. **Javadoc cleanup** (4.5), delete `TokenStreamTestHelpers` (4.6), implement or disable empty tests (4.7).
11. **Release pipeline** (1.10) — last, once everything else is stable.

Steps 1–6 should each be ≤300 lines of diff. Step 7 will not be; size it honestly. Steps 8–11 are independent and can parallelize.

---

## 8. Acceptance criteria for the reorg as a whole

Before declaring the reorg complete, all of these must hold:

1. `mvn clean verify -P live-it` green on a fresh clone with only Docker and a JDK installed.
2. `mvn dependency:analyze` reports zero unused-declared and zero used-undeclared.
3. SPI smoke test (§4.9) passes.
4. `git grep -l umich-solr-extensions -- ':!.git'` returns nothing outside explicit historical references (e.g. a changelog entry documenting the rename).
5. **Downstream sanity check**: drop the resulting JAR into a stock `solr:10` image, point a configset at `edu.umich.lib.solr.filter.AnyCallNumberNormalizerFilterFactory`, index a document, query it back. This catches the §1.1 BLOCKER if it regresses, the §1.3 BLOCKER if it regresses, and the §3.1 path-divergence if it regresses, in one ~5-minute check. Automate as a final CI job.
6. A release tag (`v1.0.0-rc1` or similar) successfully publishes to GitHub Packages and is consumable from a separate test project.
