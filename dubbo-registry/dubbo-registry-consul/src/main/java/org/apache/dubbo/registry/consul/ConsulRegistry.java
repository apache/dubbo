package org.apache.dubbo.registry.consul;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class ConsulRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);

    private static final int DEFAULT_PORT = 8500;
    private static final int DEFAULT_WATCH_TIMEOUT = 2;

    private ConsulClient client;
    private long ttl;
    private ScheduledExecutorService registerTimer = newSingleThreadScheduledExecutor(
            new NamedThreadFactory("dubbo-consul-register-timer", true));
    private ScheduledExecutorService subscriberTimer = newSingleThreadScheduledExecutor(
            new NamedThreadFactory("dubbo-consul-subscriber-timer", true));
    private ConsulNotifier notifier = new ConsulNotifier();

    public ConsulRegistry(URL url) {
        super(url);
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
        ttl = url.getParameter("ttl", 16000L);
        registerTimer.scheduleWithFixedDelay(this::keepAlive, ttl / 2, ttl / 2, TimeUnit.MILLISECONDS);
        subscriberTimer.scheduleWithFixedDelay(notifier, DEFAULT_WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT, TimeUnit.SECONDS);
    }

    private void keepAlive() {
        for (URL url : getRegistered()) {
            String checkId = buildId(url);
            try {
                client.agentCheckPass("service:" + checkId);
            } catch (Throwable t) {
                logger.warn("fail to check pass for url: " + url + ", check id is: " + checkId);
            }
        }
    }

    private String buildId(URL url) {
        return url.getServiceKey();
    }

    private NewService.Check buildCheck(URL url) {
        NewService.Check check = new NewService.Check();
        check.setTtl((ttl / 1000) + "s");
        return check;
    }

    @Override
    public void doRegister(URL url) {
        if (isConsumerSide(url)) {
            return;
        }

        NewService service = new NewService();
        service.setAddress(url.toFullString());
        service.setPort(url.getPort());
        service.setId(buildId(url));
        service.setName(url.getServiceKey());
        service.setCheck(buildCheck(url));
        service.setTags(Collections.singletonList("dubbo"));
        client.agentServiceRegister(service);
    }

    @Override
    public void doUnregister(URL url) {
        if (isConsumerSide(url)) {
            return;
        }

        client.agentServiceDeregister(buildId(url));
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        String service = url.getServiceKey();
        Response<List<HealthService>> healthServices = queryHealthServices(service, -1);
        Long index = healthServices.getConsulIndex();
        List<URL> urls = convert(healthServices.getValue());
        notify(url, listener, urls);
        notifier.watch(url, index);
    }

    private Response<List<HealthService>> queryHealthServices(String service, long index) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag("dubbo")
                .setQueryParams(new QueryParams(DEFAULT_WATCH_TIMEOUT, index))
                .setPassing(true)
                .build();
        return client.getHealthServices(service, request);
    }

    private boolean isConsumerSide(URL url) {
        return url.getProtocol().equals(Constants.CONSUMER_PROTOCOL);
    }

    private List<URL> convert(List<HealthService> services) {
        return services.stream().map(s -> s.getService().getAddress()).map(URL::valueOf).collect(Collectors.toList());
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        notifier.unwatch(url);
    }

    @Override
    public boolean isAvailable() {
        return client.getAgentSelf() != null;
    }

    @Override
    public void destroy() {
        super.destroy();
        subscriberTimer.shutdown();
        registerTimer.shutdown();
    }

    private class ConsulNotifier implements Runnable {
        private ConcurrentMap<URL, Long> lastIndexes = new ConcurrentHashMap<>();

        @Override
        public void run() {
            for (URL url : lastIndexes.keySet()) {
                String service = url.getServiceKey();
                Long lastIndex = lastIndexes.get(url);
                Response<List<HealthService>> response = queryHealthServices(service, lastIndex);
                Long index = response.getConsulIndex();
                if (index != null && !index.equals(lastIndex)) {
                    List<HealthService> services = response.getValue();
                    List<URL> urls = convert(services);
                    for (NotifyListener listener : getSubscribed().get(url)) {
                        doNotify(url, listener, urls);
                    }
                }
            }
        }

        public void watch(URL url, Long consulIndex) {
            lastIndexes.putIfAbsent(url, consulIndex);
        }

        public void unwatch(URL url) {
            lastIndexes.remove(url);
        }
    }
}
