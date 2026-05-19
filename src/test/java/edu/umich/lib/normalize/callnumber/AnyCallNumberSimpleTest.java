package edu.umich.lib.normalize.callnumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class AnyCallNumberSimpleTest {

    @ParameterizedTest
    @CsvFileSource(files = "src/test/java/edu/umich/lib/normalize/callnumber/any_valid_key_verification.tsv", delimiterString = "->")
    void any_valid_key(String original, String collated)  {
        AnyCallNumberSimple acn = new AnyCallNumberSimple(original);
        String computed = acn.anyAcceptableKey();
        if (computed == null) computed = "null";
        assertEquals(collated.toString(), computed);
    }

    @Test
    void valid_truncated_key() {
    }

    @Test
    void invalid_key() {
    }
}
