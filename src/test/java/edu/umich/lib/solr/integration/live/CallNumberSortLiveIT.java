package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Live IT for the two call-number sort field types: CallNumberSortKeyFieldType
 * and CallNumberSortableFieldType.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallNumberSortLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/callnumber-sort.jsonl");
  }

  @Test
  void callnumberSortKeyOrdersAscending() throws Exception {
    // A1 .B2 1900 < PS3537.A832 B6 1948 < QA76.73.J38 G67 2018 < ZZ999 .Z99 9999
    List<String> ids = idsOf(client.query(
        new SolrQuery("*:*").setFields("id").addSort("callnumber_sort", SolrQuery.ORDER.asc).setRows(50)));
    assertEquals(List.of("sort-c", "sort-a", "sort-b", "sort-d"), ids);
  }

  @Test
  void callnumberSortableOrdersAscending() throws Exception {
    List<String> ids = idsOf(client.query(
        new SolrQuery("*:*").setFields("id").addSort("callnumber_sortable", SolrQuery.ORDER.asc).setRows(50)));
    assertEquals(List.of("sort-c", "sort-a", "sort-b", "sort-d"), ids);
  }

  @Test
  void callnumberSortKeyOrdersDescending() throws Exception {
    List<String> ids = idsOf(client.query(
        new SolrQuery("*:*").setFields("id").addSort("callnumber_sort", SolrQuery.ORDER.desc).setRows(50)));
    assertEquals(List.of("sort-d", "sort-b", "sort-a", "sort-c"), ids);
  }

  private List<String> idsOf(QueryResponse resp) {
    return resp.getResults().stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
