package edu.umich.lib.solr.integration.live;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

/**
 * Minimal JSONL fixture loader for live IT classes.
 *
 * <p>Each line in a fixture file is a single JSON object whose top-level
 * values are strings, numbers, booleans, nulls, or string arrays. No nested
 * objects are supported (intentional: keeps the loader dependency-free).
 *
 * <p>Usage in a test class:
 *
 * <pre>{@code
 * LiveSolrFixtures.indexFromClasspath(client, "/integration/fixtures/isbn.jsonl");
 * }</pre>
 */
public final class LiveSolrFixtures {

  private LiveSolrFixtures() {}

  /**
   * Loads {@code resourcePath} from the test classpath, parses it as JSONL,
   * and posts the resulting documents (with a commit) to the given client.
   */
  public static void indexFromClasspath(SolrClient client, String resourcePath)
      throws IOException, SolrServerException {
    List<SolrInputDocument> docs = parseClasspath(resourcePath);
    if (docs.isEmpty()) {
      return;
    }
    client.add(docs);
    client.commit();
  }

  /** Parses a classpath JSONL resource into SolrInputDocuments. */
  public static List<SolrInputDocument> parseClasspath(String resourcePath) throws IOException {
    InputStream in = LiveSolrFixtures.class.getResourceAsStream(resourcePath);
    if (in == null) {
      throw new IOException("Fixture not found on classpath: " + resourcePath);
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      List<SolrInputDocument> docs = new ArrayList<>();
      String line;
      int lineno = 0;
      while ((line = reader.readLine()) != null) {
        lineno++;
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        try {
          docs.add(toSolrDoc(parseObject(trimmed)));
        } catch (RuntimeException e) {
          throw new IOException("Bad JSONL at " + resourcePath + ":" + lineno + " — " + e.getMessage(), e);
        }
      }
      return docs;
    }
  }

  private static SolrInputDocument toSolrDoc(Map<String, Object> obj) {
    SolrInputDocument doc = new SolrInputDocument();
    for (Map.Entry<String, Object> entry : obj.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Collection) {
        for (Object element : (Collection<?>) value) {
          doc.addField(entry.getKey(), element);
        }
      } else {
        doc.addField(entry.getKey(), value);
      }
    }
    return doc;
  }

  // --- Tiny JSON object parser (top-level scalars + string arrays only) ----------------

  private static final Pattern KEY = Pattern.compile("\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:");

  private static Map<String, Object> parseObject(String json) {
    Map<String, Object> out = new LinkedHashMap<>();
    int i = json.indexOf('{');
    if (i < 0) {
      throw new IllegalArgumentException("Expected '{' in: " + json);
    }
    i++;
    while (i < json.length()) {
      i = skipWs(json, i);
      if (json.charAt(i) == '}') {
        return out;
      }
      Matcher m = KEY.matcher(json);
      if (!m.find(i) || m.start() != i) {
        throw new IllegalArgumentException("Expected key at offset " + i + " in: " + json);
      }
      String key = unescape(m.group(1));
      i = m.end();
      i = skipWs(json, i);
      ValueResult vr = parseValue(json, i);
      out.put(key, vr.value);
      i = skipWs(json, vr.next);
      if (i < json.length() && json.charAt(i) == ',') {
        i++;
      }
    }
    throw new IllegalArgumentException("Unterminated object: " + json);
  }

  private static ValueResult parseValue(String json, int i) {
    char c = json.charAt(i);
    if (c == '"') {
      int end = findStringEnd(json, i + 1);
      String raw = json.substring(i + 1, end);
      return new ValueResult(unescape(raw), end + 1);
    }
    if (c == '[') {
      List<Object> arr = new ArrayList<>();
      i++;
      while (true) {
        i = skipWs(json, i);
        if (json.charAt(i) == ']') {
          return new ValueResult(arr, i + 1);
        }
        ValueResult el = parseValue(json, i);
        arr.add(el.value);
        i = skipWs(json, el.next);
        if (json.charAt(i) == ',') {
          i++;
        }
      }
    }
    if (c == 't' && json.startsWith("true", i)) {
      return new ValueResult(Boolean.TRUE, i + 4);
    }
    if (c == 'f' && json.startsWith("false", i)) {
      return new ValueResult(Boolean.FALSE, i + 5);
    }
    if (c == 'n' && json.startsWith("null", i)) {
      return new ValueResult(null, i + 4);
    }
    // number
    int start = i;
    while (i < json.length() && "-+0123456789.eE".indexOf(json.charAt(i)) >= 0) {
      i++;
    }
    String num = json.substring(start, i);
    Object parsed = num.contains(".") || num.contains("e") || num.contains("E")
        ? (Object) Double.valueOf(num)
        : (Object) Long.valueOf(num);
    return new ValueResult(parsed, i);
  }

  private static int findStringEnd(String json, int start) {
    int i = start;
    while (i < json.length()) {
      char c = json.charAt(i);
      if (c == '\\') {
        i += 2;
        continue;
      }
      if (c == '"') {
        return i;
      }
      i++;
    }
    throw new IllegalArgumentException("Unterminated string starting at " + (start - 1));
  }

  private static String unescape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '\\') {
        sb.append(c);
        continue;
      }
      i++;
      char e = s.charAt(i);
      switch (e) {
        case '"': sb.append('"'); break;
        case '\\': sb.append('\\'); break;
        case '/': sb.append('/'); break;
        case 'n': sb.append('\n'); break;
        case 'r': sb.append('\r'); break;
        case 't': sb.append('\t'); break;
        case 'b': sb.append('\b'); break;
        case 'f': sb.append('\f'); break;
        case 'u':
          sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
          i += 4;
          break;
        default: sb.append(e);
      }
    }
    return sb.toString();
  }

  private static int skipWs(String json, int i) {
    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
      i++;
    }
    return i;
  }

  private static final class ValueResult {
    final Object value;
    final int next;
    ValueResult(Object value, int next) {
      this.value = value;
      this.next = next;
    }
  }
}
