package org.apache.dubbo.metadata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.store.MetadataReport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  2018/9/14
 */
public class AbstractMetadataReportFactoryTest {

    private AbstractMetadataReportFactory serviceStoreFactory = new AbstractMetadataReportFactory() {
        @Override
        protected MetadataReport createServiceStore(URL url) {
            return new MetadataReport() {

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
        MetadataReport metadataReport1 = serviceStoreFactory.getServiceStore(url);
        MetadataReport metadataReport2 = serviceStoreFactory.getServiceStore(url);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetOneServiceStoreForIpFormat() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostAddress() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = serviceStoreFactory.getServiceStore(url1);
        MetadataReport metadataReport2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffService() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService1?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService2?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = serviceStoreFactory.getServiceStore(url1);
        MetadataReport metadataReport2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffGroup() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=aaa");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=bbb");
        MetadataReport metadataReport1 = serviceStoreFactory.getServiceStore(url1);
        MetadataReport metadataReport2 = serviceStoreFactory.getServiceStore(url2);
        Assert.assertNotEquals(metadataReport1, metadataReport2);
    }
}
