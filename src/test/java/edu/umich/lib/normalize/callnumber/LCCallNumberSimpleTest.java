package edu.umich.lib.normalize.callnumber;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LCCallNumberSimpleTest {

    @ParameterizedTest
    @CsvFileSource(files = "src/test/java/edu/umich/lib/normalize/callnumber/lc__verification.tsv", delimiterString = "->")
    void collation_key(String original, String collation) {
        LCCallNumberSimple lccs = new LCCallNumberSimple(original);
        String key = lccs.collationKey();
        if (key == null) key = "null";
        assertEquals(collation.trim(), key);
    }
}
