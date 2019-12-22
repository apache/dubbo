package org.apache.dubbo.rpc.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProtocolUtilsTest {

    @Test
    public void testGetServiceKey() {
        final String serviceName = "com.abc.demoService";
        final int port = 1001;

        assertServiceKey(port, serviceName, "1.0.0", "group");
        assertServiceKey(port, serviceName, "1.0.0", "");
        assertServiceKey(port, serviceName, "1.0.0", null);

        assertServiceKey(port, serviceName, "0.0", "group");
        assertServiceKey(port, serviceName, "0_0_0", "group");
        assertServiceKey(port, serviceName, "0.0.0", "group");
        assertServiceKey(port, serviceName, "", "group");
        assertServiceKey(port, serviceName, null, "group");

        assertServiceKey(port, serviceName, "", "");
        assertServiceKey(port, serviceName, "", null);
        assertServiceKey(port, serviceName, null, "");
        assertServiceKey(port, serviceName, null, null);

        assertServiceKey(port, serviceName, "", " ");
        assertServiceKey(port, serviceName, " ", "");
        assertServiceKey(port, serviceName, " ", " ");
    }

    private void assertServiceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        Assertions.assertEquals(
                serviceKeyOldImpl(port, serviceName, serviceVersion, serviceGroup),
                ProtocolUtils.serviceKey(port, serviceName, serviceVersion, serviceGroup)
        );
    }

    /**
     * 来自 ProtocolUtils.serviceKey(int, String, String, String) 老版本的实现，用于对比测试！
     */
    private static String serviceKeyOldImpl(int port, String serviceName, String serviceVersion, String serviceGroup) {
        StringBuilder buf = new StringBuilder();
        if (serviceGroup != null && serviceGroup.length() > 0) {
            buf.append(serviceGroup);
            buf.append("/");
        }
        buf.append(serviceName);
        if (serviceVersion != null && serviceVersion.length() > 0 && !"0.0.0".equals(serviceVersion)) {
            buf.append(":");
            buf.append(serviceVersion);
        }
        buf.append(":");
        buf.append(port);
        return buf.toString();
    }

}
