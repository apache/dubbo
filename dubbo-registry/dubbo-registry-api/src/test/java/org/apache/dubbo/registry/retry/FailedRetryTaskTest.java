package org.apache.dubbo.registry.retry;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.support.FailbackRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FailedRetryTaskTest {

    private URL url;

    private NotifyListener notifyListener;

    @Before
    public void setUp() {
        url = new URL(Constants.REGISTRY_PROTOCOL, NetUtils.getLocalHost(), NetUtils.getAvailablePort())
                .setServiceInterface(RegistryService.class.getName())
                .addParameter(Constants.CHECK_KEY, false)
                .addParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, 1000);
        notifyListener = mock(NotifyListener.class);
    }

    @Test
    public void testFailedRegisteredTask() {
        FailedMockRegistry failedMockRegistry = new FailedMockRegistry(url, "register");
        failedMockRegistry.register(url);
        assertEquals(1, failedMockRegistry.getFailedRegistered().size());
        doWait();
        assertEquals(0, failedMockRegistry.getFailedRegistered().size());
        assertEquals(1, failedMockRegistry.getRegistered().size());
    }

    @Test
    public void testFailedUnRegisteredTask() {
        FailedMockRegistry failedMockRegistry = new FailedMockRegistry(url, "unregister");
        failedMockRegistry.register(url);
        assertEquals(1, failedMockRegistry.getRegistered().size());
        failedMockRegistry.unregister(url);
        assertEquals(1, failedMockRegistry.getFailedUnregistered().size());
        doWait();
        assertEquals(0, failedMockRegistry.getFailedUnregistered().size());
        assertEquals(0, failedMockRegistry.getRegistered().size());
    }

    @Test
    public void testFailedSubscribeTask() {
        FailedMockRegistry failedMockRegistry = new FailedMockRegistry(url, "subscribe");
        failedMockRegistry.register(url);
        assertEquals(1, failedMockRegistry.getRegistered().size());
        failedMockRegistry.subscribe(url, mock(NotifyListener.class));
        assertEquals(1, failedMockRegistry.getFailedSubscribed().size());
        doWait();
        assertEquals(0, failedMockRegistry.getFailedUnsubscribed().size());
    }

    @Test
    public void testFailedUnsubscribeTask() {
        FailedMockRegistry failedMockRegistry = new FailedMockRegistry(url, "unsubscribe");
        failedMockRegistry.register(url);
        assertEquals(1, failedMockRegistry.getRegistered().size());
        NotifyListener notifyListener = mock(NotifyListener.class);
        failedMockRegistry.subscribe(url, notifyListener);
        assertEquals(1, failedMockRegistry.getSubscribed().size());
        failedMockRegistry.unsubscribe(url, notifyListener);
        assertEquals(1, failedMockRegistry.getFailedUnsubscribed().size());
        doWait();
        assertEquals(0, failedMockRegistry.getFailedUnsubscribed().size());
    }

    private void deal(AtomicInteger count) {
        if (count.get() != 0) {
            count.decrementAndGet();
            throw new RuntimeException("Retry task, please ignore ...");
        }
    }

    public void doWait() {
        long period = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD)
                + 1000 * 2;
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private class FailedMockRegistry extends FailbackRegistry {

        private AtomicInteger count = new AtomicInteger(1);

        private List<String> options;

        public FailedMockRegistry(URL url, String option) {
            super(url);
            if (StringUtils.isBlank(option)) {
                throw new IllegalArgumentException("The argument option can not be null");
            }
            options = Arrays.asList(option.split(","));
        }

        @Override
        public void doRegister(URL url) {
            if (options.contains("register")) {
                deal(count);
            }
        }

        @Override
        public void doUnregister(URL url) {
            if (options.contains("unregister")) {
                deal(count);
            }
        }

        @Override
        public void doSubscribe(URL url, NotifyListener listener) {
            if (options.contains("subscribe"))
                deal(count);
        }

        @Override
        public void doUnsubscribe(URL url, NotifyListener listener) {
            if (options.contains("unsubscribe")) {
                deal(count);
            }
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}
