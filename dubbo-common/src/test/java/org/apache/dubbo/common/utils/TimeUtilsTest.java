package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilsTest {

    @Test
    void testCurrentTimeMillis() {
        assertTrue(0 < TimeUtils.currentTimeMillis());
    }
}
