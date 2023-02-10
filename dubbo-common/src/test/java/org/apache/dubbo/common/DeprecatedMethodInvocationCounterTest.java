package org.apache.dubbo.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Tests of DeprecatedMethodInvocationCounter.
 */
class DeprecatedMethodInvocationCounterTest {
    @Test
    void testRealInvocation() {
        // Invoke a deprecated method.
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path#index?version=1.0.0&id=org.apache.dubbo.config.RegistryConfig#0");
        url.getServiceName();
        url.getServiceName();

        Map<String, Integer> record = DeprecatedMethodInvocationCounter.getInvocationRecord();
        Assertions.assertEquals(2, record.get("org.apache.dubbo.common.URL.getServiceName()"));
    }
}
