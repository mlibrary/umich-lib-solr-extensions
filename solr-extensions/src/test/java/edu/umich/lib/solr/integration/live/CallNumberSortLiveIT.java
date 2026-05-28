// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live integration tests for {@link edu.umich.lib.solr.schema.CallNumberSortKeyFieldType} and
 * {@link edu.umich.lib.solr.schema.CallNumberSortableFieldType}.
 *
 * <p>Both field types are exercised against a real Solr 10 container via {@link AbstractLiveIT}.
 * Tests cover:
 *
 * <ul>
 *   <li>Ascending and descending sort ordering across LC and Dewey call numbers.
 *   <li>Mixed LC/Dewey corpora sorted together.
 *   <li>Truncated-key inputs (letters-only stubs).
 *   <li>{@code echoInvalidInput} behaviour for invalid input.
 *   <li>{@code CallNumberSortableFieldType} stores the original display value.
 *   <li>Invalid input is dropped (field absent) when {@code echoInvalidInput=false}.
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Call-number sort field types (live IT)")
class CallNumberSortLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(
        client, "/integration/fixtures/callnumber-sort-extended.jsonl");
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private List<String> idsOf(QueryResponse resp) {
    return resp.getResults().stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }

  private QueryResponse sortQuery(String field, SolrQuery.ORDER order) throws Exception {
    return client.query(
        new SolrQuery("*:*")
            .setFields("id", "callnumber_sort", "callnumber_sortable")
            .addSort(field, order)
            .setRows(100));
  }

  // -----------------------------------------------------------------------
  // callnumber_sort (CallNumberSortKeyFieldType)
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("callnumber_sort field (CallNumberSortKeyFieldType)")
  class SortKeyFieldTests {

    @Test
    @DisplayName("LC call numbers sort ascending by classification order")
    void lcAscending() throws Exception {
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-lc-*")
                  .setFields("id")
                  .addSort("callnumber_sort", SolrQuery.ORDER.asc)
                  .setRows(50));
      // A1 < PS3537 < QA76 < ZZ999
      assertEquals(List.of("sort-lc-a", "sort-lc-b", "sort-lc-c", "sort-lc-d"), idsOf(resp));
    }

    @Test
    @DisplayName("LC call numbers sort descending")
    void lcDescending() throws Exception {
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-lc-*")
                  .setFields("id")
                  .addSort("callnumber_sort", SolrQuery.ORDER.desc)
                  .setRows(50));
      assertEquals(List.of("sort-lc-d", "sort-lc-c", "sort-lc-b", "sort-lc-a"), idsOf(resp));
    }

    @Test
    @DisplayName("Dewey call numbers sort ascending by class number")
    void deweyAscending() throws Exception {
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-dewey-*")
                  .setFields("id")
                  .addSort("callnumber_sort", SolrQuery.ORDER.asc)
                  .setRows(50));
      // 100 < 813.54 < 999.9
      assertEquals(List.of("sort-dewey-a", "sort-dewey-b", "sort-dewey-c"), idsOf(resp));
    }

    @Test
    @DisplayName(
        "truncated LC stubs (letters-only) sort before full LC call numbers of same prefix")
    void truncatedKeysSortBeforeFull() throws Exception {
      // sort-trunc-a = "A"  → truncated key for A
      // sort-trunc-b = "PS" → truncated key for PS
      // sort-lc-a    = "A1 .B2 1900" → full key starting with A
      // sort-lc-b    = "PS3537..." → full key starting with PS
      // Expected ascending: A-trunc, A1, PS-trunc, PS3537…
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-trunc-a OR id:sort-trunc-b OR id:sort-lc-a OR id:sort-lc-b")
                  .setFields("id")
                  .addSort("callnumber_sort", SolrQuery.ORDER.asc)
                  .setRows(50));
      List<String> ids = idsOf(resp);
      int idxTruncA = ids.indexOf("sort-trunc-a");
      int idxLcA = ids.indexOf("sort-lc-a");
      int idxTruncB = ids.indexOf("sort-trunc-b");
      int idxLcB = ids.indexOf("sort-lc-b");
      assertTrue(idxTruncA < idxLcA, "Truncated 'A' should sort before 'A1 .B2 1900'");
      assertTrue(idxTruncB < idxLcB, "Truncated 'PS' should sort before 'PS3537...'");
    }
  }

  // -----------------------------------------------------------------------
  // callnumber_sortable field (CallNumberSortableFieldType)
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("callnumber_sortable field (CallNumberSortableFieldType)")
  class SortableFieldTests {

    @Test
    @DisplayName("produces the same sort order as callnumber_sort for LC call numbers")
    void sameOrderAsParentForLC() throws Exception {
      List<String> bySortKey =
          idsOf(
              client.query(
                  new SolrQuery("id:sort-lc-*")
                      .setFields("id")
                      .addSort("callnumber_sort", SolrQuery.ORDER.asc)
                      .setRows(50)));
      List<String> bySortable =
          idsOf(
              client.query(
                  new SolrQuery("id:sort-lc-*")
                      .setFields("id")
                      .addSort("callnumber_sortable", SolrQuery.ORDER.asc)
                      .setRows(50)));
      assertEquals(
          bySortKey,
          bySortable,
          "callnumber_sortable should produce identical ordering to callnumber_sort");
    }

    @Test
    @DisplayName("stores the original display value (not the normalised sort key)")
    void storesOriginalDisplayValue() throws Exception {
      QueryResponse resp =
          client.query(new SolrQuery("id:sort-lc-b").setFields("callnumber_sortable").setRows(1));
      SolrDocument doc = resp.getResults().get(0);
      String stored = (String) doc.getFieldValue("callnumber_sortable");
      assertEquals(
          "PS3537.A832 B6 1948",
          stored,
          "callnumber_sortable should store the original human-readable value");
    }

    @Test
    @DisplayName("Dewey call numbers sort ascending via callnumber_sortable")
    void deweyAscending() throws Exception {
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-dewey-*")
                  .setFields("id")
                  .addSort("callnumber_sortable", SolrQuery.ORDER.asc)
                  .setRows(50));
      assertEquals(List.of("sort-dewey-a", "sort-dewey-b", "sort-dewey-c"), idsOf(resp));
    }
  }

  // -----------------------------------------------------------------------
  // Edge cases: invalid input handling
  // -----------------------------------------------------------------------

  @Nested
  @DisplayName("invalid / unparseable input (echoInvalidInput=false)")
  class InvalidInputTests {

    @Test
    @DisplayName("document with invalid call number is indexed but sort field is absent")
    void invalidCallNumberFieldAbsent() throws Exception {
      // Add a document with an unparseable call number
      SolrInputDocument doc = new SolrInputDocument();
      doc.addField("id", "invalid-cn-test");
      doc.addField("callnumber_sort", "!@#$% GARBAGE");
      doc.addField("callnumber_sortable", "!@#$% GARBAGE");
      client.add(doc);
      client.commit();

      try {
        QueryResponse resp =
            client.query(
                new SolrQuery("id:invalid-cn-test")
                    .setFields("callnumber_sort", "callnumber_sortable")
                    .setRows(1));
        assertEquals(1, resp.getResults().getNumFound());
        SolrDocument result = resp.getResults().get(0);
        // echoInvalidInput=false → field should not be stored/indexed
        assertNull(
            result.getFieldValue("callnumber_sort"),
            "callnumber_sort should be absent for invalid input");
        // CallNumberSortableFieldType also skips when hasSomeKeyAtAll=false
        assertNull(
            result.getFieldValue("callnumber_sortable"),
            "callnumber_sortable should be absent for invalid input");
      } finally {
        client.deleteById("invalid-cn-test");
        client.commit();
      }
    }

    @Test
    @DisplayName("document with truncated-only LC key is indexed (allowTruncated=true)")
    void truncatedKeyIndexed() throws Exception {
      QueryResponse resp =
          client.query(
              new SolrQuery("id:sort-trunc-b").setFields("id", "callnumber_sort").setRows(1));
      assertEquals(1, resp.getResults().getNumFound());
      assertNotNull(
          resp.getResults().get(0).getFieldValue("callnumber_sort"),
          "truncated LC key should be stored when allowTruncated=true");
    }
  }
}
