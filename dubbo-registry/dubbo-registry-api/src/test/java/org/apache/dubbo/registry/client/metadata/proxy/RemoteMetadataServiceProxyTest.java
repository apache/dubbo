package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import static java.lang.String.valueOf;

/**
 * {@link RemoteMetadataServiceProxy} Test
 *
 * @since 2.7.5
 */
public class RemoteMetadataServiceProxyTest {

    @Test
    public void testGetServiceDefinition() {
        String serviceKey = "a/org.apache.dubbo.demo.DemoService:1.0.0";

        String[] services = UrlUtils.parseServiceKey(serviceKey);
        String serviceInterface = services[1];
        // if version or group is not exist
        String group = null;
        if (StringUtils.isNotEmpty(services[0])) {
            group = services[0];
        }
        String version = null;
        if (StringUtils.isNotEmpty(services[2])) {
            version = services[2];
        }
        Assert.assertEquals(serviceInterface, "org.apache.dubbo.demo.DemoService");
        Assert.assertEquals(version, "1.0.0");
        Assert.assertEquals(group, "a");

        serviceKey = "org.apache.dubbo.demo.DemoService";

        services = UrlUtils.parseServiceKey(serviceKey);
        serviceInterface = services[1];
        // if version or group is not exist
        group = null;
        if (StringUtils.isNotEmpty(services[0])) {
            group = services[0];
        }
        version = null;
        if (StringUtils.isNotEmpty(services[2])) {
            version = services[2];
        }
        Assert.assertEquals(serviceInterface, "org.apache.dubbo.demo.DemoService");
        Assert.assertEquals(version, null);
        Assert.assertEquals(group, null);
    }
}
