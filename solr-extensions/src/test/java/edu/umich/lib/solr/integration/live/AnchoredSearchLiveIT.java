// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live IT for {@code LeftAnchoredSearchFilter} and {@code FullyAnchoredSearchFilter}: verifies
 * field analysis token output and phrase-query anchoring semantics against a running Solr
 * container.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnchoredSearchLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/anchored.jsonl");
  }

  // -------------------------------------------------------------------------
  // Phrase-query anchoring
  // -------------------------------------------------------------------------

  @Test
  void leftAnchoredMatchesStartOfTitle() throws Exception {
    List<String> ids = idsOf(query("title_left:\"The Cat\""));
    assertEquals(2, ids.size(), () -> "expected 2 hits, got " + ids);
    assertTrue(
        ids.containsAll(List.of("anchored-1", "anchored-2")),
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

  // -------------------------------------------------------------------------
  // Field analysis -- left-anchored
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Field analysis -- left-anchored type")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class FieldAnalysisLeftAnchored {

    @Test
    @DisplayName("'Bill Dueber' produces [Bill1, Dueber2] via text_leftanchored")
    void analyzeFieldValue_leftAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_leftanchored", "Bill Dueber");
      assertEquals(List.of("Bill1", "Dueber2"), tokens, "left-anchored analysis output mismatch");
    }

    @Test
    @DisplayName("Single token 'Smith' produces [Smith1]")
    void singleToken_leftAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_leftanchored", "Smith");
      assertEquals(List.of("Smith1"), tokens);
    }

    @Test
    @DisplayName("Three-token value produces positions 1, 2, 3")
    void threeTokens_leftAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_leftanchored", "one two three");
      assertEquals(List.of("one1", "two2", "three3"), tokens);
    }
  }

  // -------------------------------------------------------------------------
  // Field analysis -- fully-anchored
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("Field analysis -- fully-anchored type")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class FieldAnalysisFullyAnchored {

    @Test
    @DisplayName("'Bill Dueber' produces [Bill1, Dueber200] via text_fullyanchored")
    void analyzeFieldValue_fullyAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_fullyanchored", "Bill Dueber");
      assertEquals(
          List.of("Bill1", "Dueber200"), tokens, "fully-anchored analysis output mismatch");
    }

    @Test
    @DisplayName("Single token 'Smith' produces [Smith100] (first AND last)")
    void singleToken_fullyAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_fullyanchored", "Smith");
      assertEquals(List.of("Smith100"), tokens);
    }

    @Test
    @DisplayName("Three-token value: only the third token gets '00' suffix")
    void threeTokens_fullyAnchored() throws Exception {
      var tokens = fetchAnalyzedTokens("text_fullyanchored", "one two three");
      assertEquals(List.of("one1", "two2", "three300"), tokens);
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private QueryResponse query(String q) throws Exception {
    SolrQuery sq = new SolrQuery(q);
    sq.setFields("id");
    sq.setRows(50);
    return client.query(sq);
  }

  private List<String> idsOf(QueryResponse resp) {
    SolrDocumentList docs = resp.getResults();
    return docs.stream().map(d -> (String) d.getFieldValue("id")).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private List<String> fetchAnalyzedTokens(String fieldType, String value) throws Exception {
    var params =
        new MapSolrParams(
            Map.of(
                "analysis.fieldtype", fieldType,
                "analysis.fieldvalue", value,
                "wt", "javabin"));
    var req = new QueryRequest(params);
    req.setPath("/analysis/field");

    NamedList<Object> response = client.request(req);

    var analysis = (NamedList<Object>) response.get("analysis");
    var fieldTypes = (NamedList<Object>) analysis.get("field_types");
    var ftEntry = (NamedList<Object>) fieldTypes.get(fieldType);
    var indexSide = (NamedList<Object>) ftEntry.get("index");

    @SuppressWarnings("unchecked")
    var tokenList = (List<NamedList<Object>>) indexSide.getVal(indexSide.size() - 1);

    return tokenList.stream().map(t -> (String) t.get("text")).toList();
  }
}
