package org.apache.dubbo.metadata.support;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.ConsumerMetadataIdentifier;
import org.apache.dubbo.metadata.identifier.ProviderMetadataIdentifier;
import org.apache.dubbo.metadata.store.MetadataReport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  2018/9/14
 */
public class AbstractMetadataReportFactoryTest {

    private AbstractMetadataReportFactory metadataReportFactory = new AbstractMetadataReportFactory() {
        @Override
        protected MetadataReport createMetadataReport(URL url) {
            return new MetadataReport() {

                @Override
                public void storeProviderMetadata(ProviderMetadataIdentifier providerMetadataIdentifier, FullServiceDefinition serviceDefinition) {
                    store.put(providerMetadataIdentifier.getIdentifierKey(), JSON.toJSONString(serviceDefinition));
                }

                @Override
                public void storeConsumerMetadata(ConsumerMetadataIdentifier consumerMetadataIdentifier, Map serviceParameterMap) {
                    store.put(consumerMetadataIdentifier.getIdentifierKey(), JSON.toJSONString(serviceParameterMap));
                }

                Map<String, String> store = new ConcurrentHashMap<>();


            };
        }
    };

    @Test
    public void testGetOneMetadataReport() {
        URL url = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetOneMetadataReportForIpFormat() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostAddress() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffService() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService1?version=1.0.0&application=vic");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService2?version=1.0.0&application=vic");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertEquals(metadataReport1, metadataReport2);
    }

    @Test
    public void testGetForDiffGroup() {
        URL url1 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=aaa");
        URL url2 = URL.valueOf("zookeeper://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&group=bbb");
        MetadataReport metadataReport1 = metadataReportFactory.getMetadataReport(url1);
        MetadataReport metadataReport2 = metadataReportFactory.getMetadataReport(url2);
        Assert.assertNotEquals(metadataReport1, metadataReport2);
    }
}
