package org.apache.dubbo.compatible.common.extension;

import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ExtensionTest {

    @Test
    public void testExtensionFactory() {
        try {
            ExtensionFactory factory = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("myfactory");
            Assert.assertTrue(factory instanceof ExtensionFactory);
            Assert.assertTrue(factory instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
            Assert.assertTrue(factory instanceof MyExtensionFactory);

            ExtensionFactory spring = ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtension("spring");
            Assert.assertTrue(spring instanceof ExtensionFactory);
            Assert.assertFalse(spring instanceof com.alibaba.dubbo.common.extension.ExtensionFactory);
        } catch (IllegalArgumentException expected) {
            fail();
        }
    }
}
