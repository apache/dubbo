package org.apache.dubbo.errorcode.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests of ErrorUrlUtils.
 */
class ErrorUrlUtilsTest {
    @Test
    void testGetErrorUrl() {
        Assertions.assertEquals("https://dubbo.apache.org/faq/4/1", ErrorUrlUtils.getErrorUrl("4-1"));
    }

    @Test
    void testWronglyCodedCodes() {
        Assertions.assertEquals("", ErrorUrlUtils.getErrorUrl("4-"));
        Assertions.assertEquals("", ErrorUrlUtils.getErrorUrl("-4"));
        Assertions.assertEquals("", ErrorUrlUtils.getErrorUrl("X-X"));
        Assertions.assertEquals("", ErrorUrlUtils.getErrorUrl("12"));
        Assertions.assertEquals("", ErrorUrlUtils.getErrorUrl("X"));
    }
}
