// Copyright (c) 2026, The Regents of the University of Michigan.
// SPDX-License-Identifier: BSD-3-Clause
package edu.umich.lib.solr.integration.live;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Live IT for {@code LCCNNormalizerFilter}. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LccnNormalizerLiveIT extends AbstractLiveIT {

  @BeforeAll
  void loadFixtures() throws Exception {
    client.deleteByQuery("*:*");
    client.commit();
    LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/lccn.jsonl");
  }

  @Test
  void lccnStruct2NormalizesPrefixAndSerial() throws Exception {
    // "n 79021383" -> normalized prefix "n" + 8-digit serial "79021383"
    List<String> ids = idsOf(client.query(new SolrQuery("lccn:79021383").setFields("id")));
    assertTrue(ids.contains("lccn-1"), () -> "expected lccn-1 in " + ids);
  }

  @Test
  void lccnStruct1PadsTo8Digits() throws Exception {
    // "  2001627090" -> "2001627090" (10 digits, struct-1 keeps as-is)
    List<String> ids = idsOf(client.query(new SolrQuery("lccn:2001627090").setFields("id")));
    assertTrue(ids.contains("lccn-2"), () -> "expected lccn-2 in " + ids);
  }

  @Test
  void lccnHyphenatedSerialIsZeroPadded() throws Exception {
    // "a   68-2" -> prefix "a", serial 68 padded to "00000002" suffix; normalizer
    // canonical form for hyphenated serial is left-pad of the part after hyphen to 6 chars.
    List<String> ids = idsOf(client.query(new SolrQuery("lccn:68000002").setFields("id")));
    assertTrue(ids.contains("lccn-3"), () -> "expected lccn-3 in " + ids);
  }

  private List<String> idsOf(QueryResponse resp) {
    return resp.getResults().stream()
        .map(d -> (String) d.getFieldValue("id"))
        .collect(Collectors.toList());
  }
}
