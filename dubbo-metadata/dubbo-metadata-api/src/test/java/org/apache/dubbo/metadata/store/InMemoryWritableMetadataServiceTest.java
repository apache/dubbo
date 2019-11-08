package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.report.MetadataReportInstance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 2019-08-29
 */
public class InMemoryWritableMetadataServiceTest {

    String interfaceName = "org.apache.dubbo.metadata.store.InterfaceNameTestService2", version = "0.9.9", group = null;
    URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/?interface=" + interfaceName + "&version="
            + version + "&application=vicpubprovder&side=provider");

    @BeforeEach
    public void before() {
    }

    @Test
    public void testPublishServiceDefinition() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        inMemoryWritableMetadataService.publishServiceDefinition(url);

        String v = inMemoryWritableMetadataService.getServiceDefinition(interfaceName, version, group);
        Assertions.assertNotNull(v);
    }

    @Test
    public void testExportURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test567Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.exportURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.exportedServiceURLs.size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.exportedServiceURLs.get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testSubscribeURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test678Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.subscribeURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.subscribedServiceURLs.size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.subscribedServiceURLs.get(url.getServiceKey()).first(), url);
    }

    @Test
    public void testUnExportURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test567Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.exportURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.exportedServiceURLs.size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.exportedServiceURLs.get(url.getServiceKey()).first(), url);

        inMemoryWritableMetadataService.unexportURL(url);
        Assertions.assertTrue(inMemoryWritableMetadataService.exportedServiceURLs.size() == 0);
    }

    @Test
    public void testUnSubscribeURL() {
        InMemoryWritableMetadataService inMemoryWritableMetadataService = new InMemoryWritableMetadataService();
        URL url = URL.valueOf("subscriber://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.Test678Service?version=1.0.44&application=vicpubprovder&side=provider");
        inMemoryWritableMetadataService.subscribeURL(url);

        Assertions.assertTrue(inMemoryWritableMetadataService.subscribedServiceURLs.size() == 1);
        Assertions.assertEquals(inMemoryWritableMetadataService.subscribedServiceURLs.get(url.getServiceKey()).first(), url);

        inMemoryWritableMetadataService.unsubscribeURL(url);
        Assertions.assertTrue(inMemoryWritableMetadataService.subscribedServiceURLs.size() == 0);
    }

}
