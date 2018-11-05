package org.apache.dubbo.common.utils;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

public class ArrayUtilsTest {

    @Test
    public void isEmpty() throws Exception {
        assertTrue(ArrayUtils.isEmpty(null));
        assertTrue(ArrayUtils.isEmpty(new Object[0]));
        assertFalse(ArrayUtils.isEmpty(new Object[]{"abc"}));
    }

    @Test
    public void isNotEmpty() throws Exception {
        assertFalse(ArrayUtils.isNotEmpty(null));
        assertFalse(ArrayUtils.isNotEmpty(new Object[0]));
        assertTrue(ArrayUtils.isNotEmpty(new Object[]{"abc"}));
    }

}