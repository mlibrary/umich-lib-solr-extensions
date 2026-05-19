package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live IT for {@code LeftAnchoredSearchFilter} and
 * {@code FullyAnchoredSearchFilter}: index a small fixture and confirm
 * phrase queries anchor at the start (left) and at the start+end (full).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnchoredSearchLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/anchored.jsonl");
  }

  @Test
  void leftAnchoredMatchesStartOfTitle() throws Exception {
    List<String> ids = idsOf(query("title_left:\"The Cat\""));
    assertEquals(2, ids.size(), () -> "expected 2 hits, got " + ids);
    assertTrue(ids.containsAll(List.of("anchored-1", "anchored-2")),
        () -> "expected anchored-1 and anchored-2, got " + ids);
  }

  @Test
  void leftAnchoredDoesNotMatchInteriorPhrase() throws Exception {
    // "Catcher in the Rye" should NOT match the leading phrase "in the".
    List<String> ids = idsOf(query("title_left:\"in the\""));
    assertTrue(ids.isEmpty(), () -> "expected no hits, got " + ids);
  }

  @Test
  void fullyAnchoredRequiresExactStartAndEnd() throws Exception {
    List<String> ids = idsOf(query("title_full:\"The Cat in the Hat\""));
    assertEquals(List.of("anchored-1"), ids);
  }

  @Test
  void fullyAnchoredRejectsPrefix() throws Exception {
    List<String> ids = idsOf(query("title_full:\"The Cat\""));
    assertTrue(ids.isEmpty(), () -> "expected no hits, got " + ids);
  }

  private QueryResponse query(String q) throws Exception {
    SolrQuery sq = new SolrQuery(q);
    sq.setFields("id");
    sq.setRows(50);
    return client.query(sq);
  }

  private List<String> idsOf(QueryResponse resp) {
    SolrDocumentList docs = resp.getResults();
    return docs.stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
