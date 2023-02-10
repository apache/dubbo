package org.apache.dubbo.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests of DeprecatedMethodInvocationCounter.
 */
class DeprecatedMethodInvocationCounterTest {

    private static final String METHOD_DEFINITION = "org.apache.dubbo.common.URL.getServiceName()";

    @Test
    void testRealInvocation() {
        Assertions.assertFalse(DeprecatedMethodInvocationCounter.hasThisMethodInvoked(METHOD_DEFINITION));

        // Invoke a deprecated method.
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path#index?version=1.0.0&id=org.apache.dubbo.config.RegistryConfig#0");
        url.getServiceName();
        url.getServiceName();

        Assertions.assertTrue(DeprecatedMethodInvocationCounter.hasThisMethodInvoked(METHOD_DEFINITION));

        Map<String, Integer> record = DeprecatedMethodInvocationCounter.getInvocationRecord();
        Assertions.assertEquals(2, record.get(METHOD_DEFINITION));
    }
}
