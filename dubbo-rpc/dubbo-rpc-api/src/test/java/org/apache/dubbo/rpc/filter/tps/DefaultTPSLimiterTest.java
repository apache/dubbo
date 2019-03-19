package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author: lizhen
 * @since: 2019-03-19
 * @description:
 */
public class DefaultTPSLimiterTest {

    private DefaultTPSLimiter defaultTPSLimiter = new DefaultTPSLimiter();

    @Test
    public void testIsAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(Constants.INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(Constants.TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(Constants.TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
    }

    @Test
    public void testIsNotAllowable() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(Constants.INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(Constants.TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(Constants.TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 4; i++) {
            if (i == 3) {
                Assertions.assertFalse(defaultTPSLimiter.isAllowable(url, invocation));
            } else {
                Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
            }
        }
    }


    @Test
    public void testConfigChange() throws Exception {
        Invocation invocation = new MockInvocation();
        URL url = URL.valueOf("test://test");
        url = url.addParameter(Constants.INTERFACE_KEY, "org.apache.dubbo.rpc.file.TpsService");
        url = url.addParameter(Constants.TPS_LIMIT_RATE_KEY, 2);
        url = url.addParameter(Constants.TPS_LIMIT_INTERVAL_KEY, 1000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
        url = url.addParameter(Constants.TPS_LIMIT_RATE_KEY, 2000);
        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(defaultTPSLimiter.isAllowable(url, invocation));
        }
    }
}
