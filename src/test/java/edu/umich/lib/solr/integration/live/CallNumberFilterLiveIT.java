package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live IT for the three call-number simple filters: LC, Dewey, and Any
 * (which routes by detected scheme).
 *
 * <p>The call-number filters are designed for collation/sorting and left-anchored
 * prefix search against the normalized sort-key form. They emit a single keyword
 * token (the sort key) — they do not split a call number into independently
 * searchable terms. These tests therefore assert against that sort-key form:
 * exact matches and left-anchored prefix matches on the sort key.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallNumberFilterLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/callnumber-filter.jsonl");
  }

  // ---------------------------------------------------------------------------
  // LC sort-key form
  //
  // Fixture cn-1: "PS3537.A832 B6 1948"
  //   letters="ps", digits="3537" (length-prefix "4"), decimals=null,
  //   rest=".a832 b6 1948" -> cleanupFreetext -> "a832 b6 1948"
  //   collationKey = "ps43537 a832 b6 1948"
  //
  // Fixture cn-3: "QA76.73.J38 G67 2018"
  //   letters="qa", digits="76" (length-prefix "2"), decimals="73",
  //   rest=".j38 g67 2018" -> "j38 g67 2018"
  //   collationKey = "qa276.73 j38 g67 2018"
  // ---------------------------------------------------------------------------

  @Test
  void lcSortKey_exactMatch_findsDocument() throws Exception {
    // The query analyzer applies the same LC filter, so we supply the
    // user-facing call-number form. The query analyzer normalizes it to the
    // same sort key, then matches the indexed keyword token exactly.
    // Quote to defeat the query parser's whitespace splitting.
    SolrTestQuery q = new SolrTestQuery("callnumber_lc:\"PS3537.A832 B6 1948\"").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-1"), () -> "expected cn-1 in " + ids);
  }

  @Test
  void lcSortKey_leftAnchoredPrefix_findsDocument() throws Exception {
    // Left-anchored prefix search against the sort-key form.
    SolrTestQuery q = new SolrTestQuery("callnumber_lc:ps43537*").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-1"), () -> "expected cn-1 in " + ids);
  }

  @Test
  void lcSortKey_letterPrefix_findsAllLcDocuments() throws Exception {
    // "qa*" should match cn-3 ("qa276.73 ..."); "ps*" should match cn-1.
    List<String> qaIds = idsOf(client.query(new SolrTestQuery("callnumber_lc:qa*").setFields("id")));
    assertTrue(qaIds.contains("cn-3"), () -> "expected cn-3 in " + qaIds);

    List<String> psIds = idsOf(client.query(new SolrTestQuery("callnumber_lc:ps*").setFields("id")));
    assertTrue(psIds.contains("cn-1"), () -> "expected cn-1 in " + psIds);
  }

  // ---------------------------------------------------------------------------
  // Dewey sort-key form
  //
  // Fixture cn-2: "813.54 SAL"
  //   digits="813", decimals=".54", rest=" SAL" -> "sal"
  //   collationKey = "813.54 sal"
  // ---------------------------------------------------------------------------

  @Test
  void deweySortKey_exactMatch_findsDocument() throws Exception {
    SolrTestQuery q = new SolrTestQuery("callnumber_dewey:\"813.54 SAL\"").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-2"), () -> "expected cn-2 in " + ids);
  }

  @Test
  void deweySortKey_leftAnchoredPrefix_findsDocument() throws Exception {
    // Three-digit prefix is an acceptable truncated key for Dewey, but the
    // indexed term is the full collation key. Prefix-match against that.
    SolrTestQuery q = new SolrTestQuery("callnumber_dewey:813*").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-2"), () -> "expected cn-2 in " + ids);
  }

  // ---------------------------------------------------------------------------
  // Any-call-number routing
  //
  // For LC-shaped input, Any produces the LC sort key.
  // For Dewey-shaped input, Any produces the Dewey sort key.
  // ---------------------------------------------------------------------------

  @Test
  void anyCallNumber_routesLcInputToLcSortKey() throws Exception {
    // Supply the user-facing call-number form; the query analyzer normalizes
    // it to the same sort key as indexing.
    SolrTestQuery q = new SolrTestQuery("callnumber_any:\"PS3537.A832 B6 1948\"").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-1"), () -> "expected cn-1 in " + ids);
  }

  @Test
  void anyCallNumber_routesDeweyInputToDeweySortKey() throws Exception {
    SolrTestQuery q = new SolrTestQuery("callnumber_any:\"813.54 SAL\"").setFields("id");
    List<String> ids = idsOf(client.query(q));
    assertTrue(ids.contains("cn-2"), () -> "expected cn-2 in " + ids);
  }

  @Test
  void allFixturesIndexedAsSingleKeywordTokens() throws Exception {
    // Sanity check: every fixture document is present (the filter did not
    // drop tokens silently). Three docs, one query.
    QueryResponse resp = client.query(new SolrTestQuery("*:*").setFields("id").setRows(10));
    List<String> ids = idsOf(resp);
    assertEquals(3, ids.size(), () -> "expected 3 fixture docs, got " + ids);
    assertTrue(ids.contains("cn-1") && ids.contains("cn-2") && ids.contains("cn-3"),
        () -> "expected cn-1, cn-2, cn-3 in " + ids);
  }

  private List<String> idsOf(QueryResponse resp) {
    return resp.getResults().stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
