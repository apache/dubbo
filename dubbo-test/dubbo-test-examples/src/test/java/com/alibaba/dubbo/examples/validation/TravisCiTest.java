package com.alibaba.dubbo.examples.validation;

import org.junit.Assert;
import org.junit.Test;

public class TravisCiTest {
    @Test
    public void testFail() {
        Assert.assertEquals(true,false);
    }
}
