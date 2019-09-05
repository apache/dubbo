package org.apache.dubbo.common.extension.circle;

import org.apache.dubbo.common.extension.ExtensionLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestCircle {

    @Test
    public void test_circle() {
        ASpi ASpi = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.circle.ASpi.class).getAdaptiveExtension();
        BSpi BSpi = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.circle.BSpi.class).getAdaptiveExtension();
        Assertions.assertEquals(BSpi.getASpi(), ASpi);
        Assertions.assertEquals(ASpi.getBSpi(), BSpi);
    }
}
