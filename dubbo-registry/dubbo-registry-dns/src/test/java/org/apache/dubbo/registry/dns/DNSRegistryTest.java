package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class DNSRegistryTest {
    private String service = "io.grpc.examples.helloworld.GreeterGrpc$IGreeter";
    //    private String service = "*";
    //root 为 dns
    private URL serviceUrl = URL.valueOf("dns://mydns:8848/" + service + "?notify=false&methods=test1,test2#fragment");
    //    private URL registryUrl;
    private DNSRegistry registry;

    @BeforeEach
    public void setUp() {
        int port = 8848;
//        this.registryUrl = URL.valueOf("dns://localhost:" + port);
        DNSRegistryFactory registryFactory = new DNSRegistryFactory();
        this.registry = (DNSRegistry) registryFactory.createRegistry(this.serviceUrl);
    }

    @Test
    public void testRegister() {
        Set<URL> registered = null;

        for (int i = 0; i < 2; i++) {
            registry.register(serviceUrl);
            registered = registry.getRegistered();
            assertThat(registered.contains(serviceUrl), is(true));
        }

        registered = registry.getRegistered();
        assertThat(registered.size(), is(1));
    }

    @Test
    public void testSubscribe() {
        NotifyListener listener = mock(NotifyListener.class);
        registry.subscribe(serviceUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

        registry.unsubscribe(serviceUrl, listener);
        subscribed = registry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(0));
    }
    @Test
    public void testAnyHost() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
            new DNSRegistryFactory().createRegistry(errorUrl);
        });
    }

    @Test
    public void testSubscribeAndUnsubscribe() {
        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {

            }
        };
        registry.subscribe(serviceUrl, listener);
        Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();

        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

        registry.unsubscribe(serviceUrl, listener);
        subscribed = registry.getSubscribed();
        assertThat(subscribed.get(serviceUrl).size(), is(0));
    }

    @Test
    public void testAvailable() {
        registry.register(serviceUrl);
        assertThat(registry.isAvailable(), is(true));

        registry.destroy();
        assertThat(registry.isAvailable(), is(false));
    }
}
