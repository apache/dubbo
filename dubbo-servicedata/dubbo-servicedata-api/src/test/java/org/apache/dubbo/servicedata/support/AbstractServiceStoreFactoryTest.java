package org.apache.dubbo.servicedata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.servicedata.ServiceStore;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cvictory ON 2018/9/14
 */
public class AbstractServiceStoreFactoryTest {

    private AbstractServiceStoreFactory serviceStoreFactory = new AbstractServiceStoreFactory() {
        @Override
        protected ServiceStore createServiceStore(URL url) {
            return new ServiceStore() {

                Map<String, String> store = new ConcurrentHashMap<>();

                @Override
                public void put(URL url) {
                    store.put(url.getServiceKey(), url.toParameterString());
                }

                @Override
                public URL peek(URL url) {
                    String queryV = store.get(url.getServiceKey());
                    return url.clearParameters().addParameterString(queryV);
                }
            };
        }
    };

    @Test
    public void testGetOneServiceStore() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        ServiceStore serviceStore1 = serviceStoreFactory.getServiceStore(url);
        ServiceStore serviceStore2 = serviceStoreFactory.getServiceStore(url);
        Assert.assertEquals(serviceStore1, serviceStore2);
    }

    @Test
    public void testGetOneServiceStoreForIpFormat() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostAddress() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        ServiceStore serviceStore1 = serviceStoreFactory.getServiceStore(url1);
        ServiceStore serviceStore2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertEquals(serviceStore1, serviceStore2);
    }

    @Test
    public void testGetForDiffService() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService1?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService2?version=1.0.0&application=vic");
        ServiceStore serviceStore1 = serviceStoreFactory.getServiceStore(url1);
        ServiceStore serviceStore2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertEquals(serviceStore1, serviceStore2);
    }

    @Test
    public void testGetForDiffGroup() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=aaa");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=bbb");
        ServiceStore serviceStore1 = serviceStoreFactory.getServiceStore(url1);
        ServiceStore serviceStore2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertNotEquals(serviceStore1, serviceStore2);
    }
}
