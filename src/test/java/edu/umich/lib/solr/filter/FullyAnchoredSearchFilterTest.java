package edu.umich.lib.solr.filter;


import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.junit.jupiter.api.Test;

import edu.umich.lib.solr.testing.ManualTokenStream;
import edu.umich.lib.solr.testing.TokenStreamTestHelpers;

import static edu.umich.lib.solr.testing.TokenStreamTestHelpers.getTerms;
import static edu.umich.lib.solr.testing.TokenStreamTestHelpers.getNestedTerms;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;

public class FullyAnchoredSearchFilterTest {

  @Test
  public void testNested() throws IOException {
        ManualTokenStream ts = new ManualTokenStream();

    ts.add("Bill", 1);
    ts.add("John", 2);
    ts.add("James", 2);
    ts.add("Dueber", 3);

    FullyAnchoredSearchFilter ff = new FullyAnchoredSearchFilter(ts);
    List<String[]> terms = getNestedTerms(ff);
    assertEquals("Bill1", terms.get(0)[0]);
    assertEquals("John2", terms.get(1)[0]);    
    assertEquals("James2", terms.get(1)[1]);
    assertEquals("Dueber300", terms.get(2)[0]);
  }

}
