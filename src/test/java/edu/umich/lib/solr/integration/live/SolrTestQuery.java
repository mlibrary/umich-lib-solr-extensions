package edu.umich.lib.solr.integration.live;

import org.apache.solr.common.params.ModifiableSolrParams;

/**
 * Stand-in for solrj's {@code SolrQuery}, whose package moved between Solr 9
 * ({@code org.apache.solr.client.solrj}) and Solr 10
 * ({@code org.apache.solr.client.solrj.request}). {@link ModifiableSolrParams}
 * lives at the same path in both, so subclassing it here keeps these Live
 * ITs source-compatible with either solrj version.
 */
final class SolrTestQuery extends ModifiableSolrParams {

  enum Order { asc, desc }

  SolrTestQuery(String q) {
    set("q", q);
  }

  SolrTestQuery setFields(String... fields) {
    set("fl", String.join(",", fields));
    return this;
  }

  SolrTestQuery setRows(int rows) {
    set("rows", rows);
    return this;
  }

  SolrTestQuery addSort(String field, Order order) {
    String clause = field + " " + order;
    String existing = get("sort");
    set("sort", existing == null ? clause : existing + "," + clause);
    return this;
  }
}
