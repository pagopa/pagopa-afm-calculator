package it.gov.pagopa.afm.calculator.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static it.gov.pagopa.afm.calculator.util.CalculatorUtil.*;

public class CalculatorUtilTest {

    @Test
    void parameterTest() {
        Assertions.assertTrue(getAllCCP(null));
        Assertions.assertTrue(getAllCCP(""));
        Assertions.assertTrue(getAllCCP("true"));
        Assertions.assertFalse(getAllCCP("false"));
        Assertions.assertFalse(getAllCCP("randomString"));

        Assertions.assertTrue(getOnUsFirst(null));
        Assertions.assertTrue(getOnUsFirst(""));
        Assertions.assertTrue(getOnUsFirst("true"));
        Assertions.assertFalse(getOnUsFirst("false"));
        Assertions.assertFalse(getOnUsFirst("randomString"));

        Assertions.assertEquals("random", getOrderBy(null));
        Assertions.assertEquals("random", getOrderBy(""));
        Assertions.assertEquals("fee", getOrderBy("fee"));
        Assertions.assertEquals("pspname", getOrderBy("pspname"));
    }
}
