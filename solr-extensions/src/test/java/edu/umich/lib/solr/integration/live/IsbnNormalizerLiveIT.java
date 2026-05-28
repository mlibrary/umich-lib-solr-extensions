// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Live IT for {@code ISBNNormalizerFilter}. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsbnNormalizerLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/isbn.jsonl");
  }

  @Test
  void isbn10WithDashesIndexesAsNormalizedIsbn13() throws Exception {
    // 0-306-40615-2 -> 9780306406157
    List<String> ids = idsOf(client.query(new SolrQuery("isbn:9780306406157").setFields("id")));
    assertTrue(ids.contains("isbn-1"), () -> "expected isbn-1 in " + ids);
  }

  @Test
  void isbn13WithDashesIndexesAsNormalizedIsbn13() throws Exception {
    // 978-0-306-40615-7 -> 9780306406157
    List<String> ids = idsOf(client.query(new SolrQuery("isbn:9780306406157").setFields("id")));
    assertTrue(ids.contains("isbn-2"), () -> "expected isbn-2 in " + ids);
  }

  @Test
  void invalidIsbnIsEchoedAsToken() throws Exception {
    // isbn-3 has 0123456789, an invalid checksum; echoInvalidInput=true keeps it as a literal.
    List<String> ids = idsOf(client.query(new SolrQuery("isbn:0123456789").setFields("id")));
    assertEquals(List.of("isbn-3"), ids);
  }

  private List<String> idsOf(QueryResponse resp) {
    return resp.getResults().stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
