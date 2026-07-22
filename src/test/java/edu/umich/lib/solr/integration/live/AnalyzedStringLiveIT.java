package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live IT for AnalyzedString field type.
 *
 * <p>AnalyzedString extends StrField: values fed to the field are run through
 * the index analysis chain of a referenced TextField and stored as a plain
 * string. This guarantees that range queries compare pre-normalized values.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyzedStringLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/browse.jsonl");
  }

  /**
   * Values stored in an AnalyzedString field are normalized at index time:
   * uppercase → lowercase, accented chars → ASCII equivalents.
   */
  @Test
  void storedValuesAreNormalized() throws Exception {
    QueryResponse resp = client.query(new SolrTestQuery("id:browse-1").setFields("author_browse"));
    assertEquals("beatles",
        resp.getResults().get(0).getFieldValue("author_browse"),
        "Beatles should be stored as 'beatles'");

    resp = client.query(new SolrTestQuery("id:browse-5").setFields("author_browse"));
    assertEquals("bjork",
        resp.getResults().get(0).getFieldValue("author_browse"),
        "Bjork should be stored as 'bjork' (ASCII folded)");
  }

  /**
   * Range queries on an AnalyzedString field compare pre-normalized values.
   */
  @Test
  void rangeQueryMatchesNormalizedValues() throws Exception {
    // Normalized order: aerosmith, beatles, bjork, rolling stones, zz top
    // [bjork TO rolling stones] should return bjork and rolling stones
    QueryResponse resp = client.query(
        new SolrTestQuery("author_browse:[bjork TO \"rolling stones\"]").setRows(10));
    List<String> ids = idsOf(resp);
    assertEquals(2, ids.size(), "Expected 2 docs in range [bjork TO rolling stones]");
    assert ids.contains("browse-3") : "rolling stones should be in range";
    assert ids.contains("browse-5") : "bjork should be in range";
  }

  /**
   * Open-lower-bound range: everything up to and including beatles.
   */
  @Test
  void openLowerBoundRange() throws Exception {
    // aerosmith and beatles are <= beatles
    QueryResponse resp = client.query(
        new SolrTestQuery("author_browse:[* TO beatles]").setRows(10));
    List<String> ids = idsOf(resp);
    assertEquals(2, ids.size(), "Expected 2 docs with author_browse <= beatles");
    assert ids.contains("browse-1") : "beatles should be included";
    assert ids.contains("browse-2") : "aerosmith should be included";
  }

  /**
   * Open-upper-bound range: everything from rolling stones onward.
   */
  @Test
  void openUpperBoundRange() throws Exception {
    // rolling stones and zz top are >= rolling stones
    QueryResponse resp = client.query(
        new SolrTestQuery("author_browse:[\"rolling stones\" TO *]").setRows(10));
    List<String> ids = idsOf(resp);
    assertEquals(2, ids.size(), "Expected 2 docs with author_browse >= rolling stones");
    assert ids.contains("browse-3") : "rolling stones should be included";
    assert ids.contains("browse-4") : "zz top should be included";
  }

  private List<String> idsOf(QueryResponse resp) {
    SolrDocumentList results = resp.getResults();
    return results.stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
