package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.junit.Rule;
import org.junit.Test;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.NotifyListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.*;
import org.apache.dubbo.registry.support.AbstractRegistry;

/**
 * Created by yufeiwu on 2018/8/6.
 */
public class AbstractRegistryTest {
    static String service;
    static URL serviceUrl;
    static URL registryUrl;
    static URL nullURL=null;
    //FailbackRegistryTest.MockRegistry registry;
    private int FAILED_PERIOD = 200;
    private int sleeptime = 100;
    private int trytimes = 5;
    //private final Set<URL> registered = new ConcurrentHashSet<URL>();
    static AbstractRegistry abstractRegistry;
    static List<URL> urls;
    static ConcurrentMap<URL, Set<NotifyListener>> subscribed;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        service = "org.apache.dubbo.test.DemoService";
        serviceUrl = URL.valueOf("remote://127.0.0.1/demoservice?method=get");
        registryUrl = URL.valueOf("http://1.2.3.4:9090/registry?check=false&file=N/A").addParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, String.valueOf(FAILED_PERIOD));
        abstractRegistry=new AbstractRegistry(serviceUrl) {
            @Override
            public boolean isAvailable() {
                return false;
            }
        };
        urls=new ArrayList();
        subscribed=new ConcurrentHashMap<URL, Set<NotifyListener>>();

    }
    @Test
    public void test_register_null() throws Exception{
        try {
            abstractRegistry.register(nullURL);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("register url == null", expected.getMessage());
        }
    }

    @Test
    public  void register()throws Exception{
        abstractRegistry.register(registryUrl);
        System.out.println("register"+registryUrl);
        assertTrue(abstractRegistry.getRegistered().contains(registryUrl));

    }
    @Test
    public void test_unregister_null() throws Exception{
        try {
            abstractRegistry.unregister(nullURL);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("unregister url == null", expected.getMessage());
        }
    }

    @Test
    public void unregister() throws Exception {
        abstractRegistry.unregister(registryUrl);
        System.out.println("unregister"+registryUrl);
        assertFalse(abstractRegistry.getRegistered().contains(registryUrl));
    }

    @Test
    public void test_subscribe_null() throws Exception{
        try {
            NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
            abstractRegistry.subscribe(nullURL,notifyListener);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("subscribe url == null", expected.getMessage());
        }
        try {
            abstractRegistry.subscribe(registryUrl,null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("subscribe listener == null", expected.getMessage());
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
        System.out.println("Subscribe: "+registryUrl);
        abstractRegistry.subscribe(registryUrl,notifyListener);
        assertTrue(abstractRegistry.getSubscribed().get(registryUrl).contains(notifyListener));
    }


    @Test
    public void test_unsubscribe_null() throws Exception{
        try {
            NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
            abstractRegistry.unsubscribe(nullURL,notifyListener);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("unsubscribe url == null", expected.getMessage());
        }
        try {
            abstractRegistry.unsubscribe(registryUrl,null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("unsubscribe listener == null", expected.getMessage());
        }
    }


    @Test
    public void testUnsubscribe() throws Exception {
        NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
        System.out.println("unsubscribe: "+registryUrl);
        abstractRegistry.subscribe(registryUrl,notifyListener);
        abstractRegistry.unsubscribe(registryUrl,notifyListener);
        assertFalse(abstractRegistry.getSubscribed().get(registryUrl).contains(notifyListener));
    }

    @Test
    public void recover() throws Exception {
        NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
        abstractRegistry.register(registryUrl);
        abstractRegistry.register(serviceUrl);
        assertEquals(1,abstractRegistry.getRegistered().size());
        assertEquals(1,abstractRegistry.getSubscribed().size());
        abstractRegistry.recover();
        assertEquals(1,abstractRegistry.getRegistered().size());
        assertEquals(1,abstractRegistry.getSubscribed().size());

    }

    /*@Test
    public void notify() throws Exception {
    }*/


    @Test
    public void test_notify_null() throws Exception{
        try {
            NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
            abstractRegistry.notify(nullURL,notifyListener,urls);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("notify url == null", expected.getMessage());
        }
        try {
            abstractRegistry.notify(registryUrl,null,urls);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("notify listener == null", expected.getMessage());
        }
    }
    @Test
    public void notify1() throws Exception {
        NotifyListener notifyListener=Mockito.mock(NotifyListener.class);
        AbstractRegistry abstractRegistry=Mockito.mock(AbstractRegistry.class);
        abstractRegistry.notify(registryUrl,notifyListener,urls);
        Mockito.verify(abstractRegistry).notify(registryUrl,notifyListener,urls);

    }

}