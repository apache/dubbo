package org.apache.dubbo.servicedata.integration;

import com.sun.source.tree.AssertTree;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.servicedata.store.test.JTestServiceStore4Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author cvictory ON 2018/9/14
 */
public class ServiceStoreServiceTest {
    URL url = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
    ServiceStoreService serviceStoreService1;

    @Before
    public void before() {

        serviceStoreService1 = ServiceStoreService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
    }

    @Test
    public void testInstance() {

        ServiceStoreService serviceStoreService2 = ServiceStoreService.instance(new Supplier<URL>() {
            @Override
            public URL get() {
                return url;
            }
        });
        Assert.assertSame(serviceStoreService1, serviceStoreService2);
        Assert.assertEquals(serviceStoreService1.serviceStoreUrl, url);
    }

    @Test
    public void testPublishProviderNoInterfaceName() {


        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder");
        serviceStoreService1.publishProvider(publishUrl);

        Assert.assertTrue(serviceStoreService1.serviceStore instanceof JTestServiceStore4Test);
        Assert.assertTrue(serviceStoreService1.providerURLs.size() >= 1);

        JTestServiceStore4Test jTestServiceStore4Test = (JTestServiceStore4Test) serviceStoreService1.serviceStore;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestServiceStore4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestServiceStore4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubprovder");
        Assert.assertEquals(map.get("version"), "1.0.0");

    }

    @Test
    public void testPublishProviderWrongInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpu&interface=ccc");
        serviceStoreService1.publishProvider(publishUrl);

        Assert.assertTrue(serviceStoreService1.serviceStore instanceof JTestServiceStore4Test);
        Assert.assertTrue(serviceStoreService1.providerURLs.size() >= 1);

        JTestServiceStore4Test jTestServiceStore4Test = (JTestServiceStore4Test) serviceStoreService1.serviceStore;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestServiceStore4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestServiceStore4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpu");
        Assert.assertEquals(map.get("version"), "1.0.0");
        Assert.assertEquals(map.get("interface"), "ccc");
        Assert.assertNull(map.get(Constants.SERVICE_DESCIPTOR_KEY));
    }

    @Test
    public void testPublishProviderContainInterface() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.3&application=vicpubp&interface=org.apache.dubbo.servicedata.integration.InterfaceNameTestService");
        serviceStoreService1.publishProvider(publishUrl);

        Assert.assertTrue(serviceStoreService1.serviceStore instanceof JTestServiceStore4Test);
        Assert.assertTrue(serviceStoreService1.providerURLs.size() >= 1);

        JTestServiceStore4Test jTestServiceStore4Test = (JTestServiceStore4Test) serviceStoreService1.serviceStore;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestServiceStore4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestServiceStore4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubp");
        Assert.assertEquals(map.get("version"), "1.0.3");
        Assert.assertEquals(map.get("interface"), "org.apache.dubbo.servicedata.integration.InterfaceNameTestService");
        Assert.assertNotNull(map.get(Constants.SERVICE_DESCIPTOR_KEY));
    }

    @Test
    public void testPublishConsumer() {

        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.x&application=vicpubconsumer&side=consumer");
        serviceStoreService1.publishConsumer(publishUrl);

        Assert.assertTrue(serviceStoreService1.serviceStore instanceof JTestServiceStore4Test);
        Assert.assertTrue(serviceStoreService1.consumerURLs.size() >= 1);

        JTestServiceStore4Test jTestServiceStore4Test = (JTestServiceStore4Test) serviceStoreService1.serviceStore;
        Assert.assertTrue(jTestServiceStore4Test.store.containsKey(JTestServiceStore4Test.getKey(publishUrl)));

        String value = jTestServiceStore4Test.store.get(JTestServiceStore4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), "vicpubconsumer");
        Assert.assertEquals(map.get("version"), "1.0.x");

    }

    @Test
    public void testPublishAll() {
        //need single url
        URL urlTmp = URL.valueOf("JTest://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestSingleService?version=1.x.x&application=vicss");
        ServiceStoreService serviceStoreService2 = new ServiceStoreService(urlTmp);
        URL publishUrl = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vicpubprovder");
        serviceStoreService2.publishProvider(publishUrl);
        serviceStoreService2.publishProvider(publishUrl);
        URL publishUrl2 = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.3&application=vicpubp&interface=org.apache.dubbo.servicedata.integration.InterfaceNameTestService");
        serviceStoreService2.publishProvider(publishUrl2);
        URL publishUrl3 = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.x&application=vicpubconsumer&side=consumer");
        serviceStoreService2.publishConsumer(publishUrl3);

        Assert.assertTrue(serviceStoreService2.providerURLs.size() == 2);
        Assert.assertTrue(serviceStoreService2.consumerURLs.size() == 1);

        JTestServiceStore4Test jTestServiceStore4Test = (JTestServiceStore4Test) serviceStoreService2.serviceStore;
        checkParam(jTestServiceStore4Test, publishUrl, "vicpubprovder", "1.0.0", null);
        checkParam(jTestServiceStore4Test, publishUrl2, "vicpubp", "1.0.3", "org.apache.dubbo.servicedata.integration.InterfaceNameTestService");
        checkParam(jTestServiceStore4Test, publishUrl3, "vicpubconsumer", "1.0.x", null);

        Assert.assertTrue(jTestServiceStore4Test.store.size() == 3);


        jTestServiceStore4Test.store.clear();
        Assert.assertTrue(jTestServiceStore4Test.store.isEmpty());

        //test
        serviceStoreService2.publishAll();
        Assert.assertTrue(jTestServiceStore4Test.store.size() == 3);

        checkParam(jTestServiceStore4Test, publishUrl, "vicpubprovder", "1.0.0", null);
        checkParam(jTestServiceStore4Test, publishUrl2, "vicpubp", "1.0.3", "org.apache.dubbo.servicedata.integration.InterfaceNameTestService");
        checkParam(jTestServiceStore4Test, publishUrl3, "vicpubconsumer", "1.0.x", null);
    }

    private void checkParam(JTestServiceStore4Test jTestServiceStore4Test, URL publishUrl, String application, String version, String interfaceName) {
        String value = jTestServiceStore4Test.store.get(JTestServiceStore4Test.getKey(publishUrl));
        Map<String, String> map = queryUrlToMap(value);
        Assert.assertEquals(map.get("application"), application);
        Assert.assertEquals(map.get("version"), version);
        Assert.assertEquals(map.get("interface"), interfaceName);
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
