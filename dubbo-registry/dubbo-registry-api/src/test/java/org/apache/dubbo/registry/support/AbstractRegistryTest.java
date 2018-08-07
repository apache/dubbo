package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AbstractRegistryTest {
    private String service = "org.apache.dubbo.test.injvmServie";
    private URL registryUrl = URL.valueOf("register://register/" + service );
    private URL anyUrl = URL.valueOf("register://register/*");
    private AbstractRegistry registry = new AbstractRegistry(registryUrl) {
        @Override
        public boolean isAvailable() {
            return false;
        }
    };

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull(){
        registry.register(null);
    }

    @Test
    public void testRegister(){
        Set<URL> registered;

        for (int i = 0; i < 2; i++) {
            registry.register(registryUrl);
            registered = registry.getRegistered();
            assertThat(registered.contains(registryUrl), is(true));
        }

        registered = registry.getRegistered();
        assertThat(registered.size(), is(1));
    }

    @Test
    public void testUnRegister(){
        registry.register(registryUrl);
        registry.unregister(registryUrl);
        Assert.assertEquals(0, registry.getRegistered().size());
    }

    @Test
    public void testSubscribe(){
        NotifyListener listener = mock(NotifyListener.class);
        registry.subscribe(registryUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(registryUrl).size(), is(1));
    }

    @Test
    public void testUnSubscribe(){
        NotifyListener listener = mock(NotifyListener.class);
        registry.subscribe(registryUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
        registry.unsubscribe(registryUrl, listener);
        subscribed = registry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(registryUrl).size(), is(0));
    }

    @Test
    public void testRecover() throws Exception {
        registry.register(registryUrl);
        Set<URL> registered = registry.getRegistered();
        NotifyListener listener = mock(NotifyListener.class);
        registry.subscribe(registryUrl, listener);
        Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
        registry.recover();

        Assert.assertEquals(registered, registry.getRegistered());
        Assert.assertEquals(subscribed, registry.getSubscribed());
    }

    @Test
    public void testNotify() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        registry.subscribe(anyUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                latch.countDown();
            }
        });

        registry.notify(Arrays.asList(registryUrl));
        latch.await();
    }

    @Test
    public void testNotifyWithListener() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        registry.notify(anyUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                latch.countDown();
            }
        },Arrays.asList(registryUrl));
        latch.await();
    }

}
