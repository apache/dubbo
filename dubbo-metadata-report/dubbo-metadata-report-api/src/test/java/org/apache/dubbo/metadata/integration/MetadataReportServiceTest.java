package org.apache.dubbo.metadata.integration;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.store.test.JTestMetadataReport4Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 2018/9/14
 */
public class MetadataReportServiceTest {
    URL url = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
    MetadataReportService metadataReportService1;

    @Before
    public void before() {

        metadataReportService1 = MetadataReportService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
    }

    @Test
    public void testInstance() {

        MetadataReportService metadataReportService2 = MetadataReportService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
        Assert.assertSame(metadataReportService1, metadataReportService2);
        Assert.assertEquals(metadataReportService1.metadataReportUrl, url);
    }

    @Test
    public void testPublishProviderNoInterfaceName() {


        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder");
        metadataReportService1.publishProvider(publishUrl);

        Assert.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestServiceStore4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestMetadataReport4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestMetadataReport4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubprovder");
        Assert.assertEquals(map.get("version"), "1.0.0");

    }

    @Test
    public void testPublishProviderWrongInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpu&interface=ccc");
        metadataReportService1.publishProvider(publishUrl);

        Assert.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestServiceStore4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestMetadataReport4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestMetadataReport4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpu");
        Assert.assertEquals(map.get("version"), "1.0.0");
        Assert.assertEquals(map.get("interface"), "ccc");
        Assert.assertNull(map.get(Constants.SERVICE_DESCIPTOR_KEY));
    }

    @Test
    public void testPublishProviderContainInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.3&application=vicpubp&interface=org.apache.dubbo.metadata.integration.InterfaceNameTestService");
        metadataReportService1.publishProvider(publishUrl);

        Assert.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestServiceStore4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestMetadataReport4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestMetadataReport4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubp");
        Assert.assertEquals(map.get("version"), "1.0.3");
        Assert.assertEquals(map.get("interface"), "org.apache.dubbo.metadata.integration.InterfaceNameTestService");
        Assert.assertNotNull(map.get(Constants.SERVICE_DESCIPTOR_KEY));
    }

    @Test
    public void testPublishConsumer() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.x&application=vicpubconsumer&side=consumer");
        metadataReportService1.publishConsumer(publishUrl);

        Assert.assertTrue(metadataReportService1.metadataReport instanceof JTestMetadataReport4Test);

        JTestMetadataReport4Test jTestServiceStore4Test = (JTestMetadataReport4Test) metadataReportService1.metadataReport;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestMetadataReport4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestMetadataReport4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubconsumer");
        Assert.assertEquals(map.get("version"), "1.0.x");

    }

    private Map<String, String> queryUrlToMap(String urlQuery) {
        if (urlQuery == null) {
            return Collections.emptyMap();
        }
        String[] pairs = urlQuery.split("&");
        Map<String, String> map = new HashMap<>();
        for (String pairStr : pairs) {
            String[] pair = pairStr.split("=");
            map.put(pair[0], pair[1]);
        }
        return map;
    }

}
