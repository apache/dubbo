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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.Constants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.Constants.PATH_SEPARATOR;
import static org.apache.dubbo.configcenter.DynamicConfiguration.DEFAULT_GROUP;

public class ConsulRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);

    private static final String SERVICE_TAG = "dubbo";
    private static final String URL_META_KEY = "url";
    private static final String WATCH_TIMEOUT = "consul-watch-timeout";
    private static final String CHECK_PASS_INTERVAL = "consul-check-pass-interval";
    private static final int DEFAULT_PORT = 8500;
    // default watch timeout in millisecond
    private static final int DEFAULT_WATCH_TIMEOUT = 2000;
    // default time-to-live in millisecond
    private static final long DEFAULT_CHECK_PASS_INTERVAL = 16000L;

    private ConsulClient client;
    private long checkPassInterval;
    private String rootPath;
    private ScheduledExecutorService registerTimer = newSingleThreadScheduledExecutor(
            new NamedThreadFactory("dubbo-consul-register-timer", true));
    private ExecutorService notifierExecutor = newCachedThreadPool(
            new NamedThreadFactory("dubbo-consul-notifier", true));
    private ConcurrentMap<URL, ConsulNotifier> notifiers = new ConcurrentHashMap<>();

    public ConsulRegistry(URL url) {
        super(url);
        this.rootPath = url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP);
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
        checkPassInterval = url.getParameter(CHECK_PASS_INTERVAL, DEFAULT_CHECK_PASS_INTERVAL);
        registerTimer.scheduleWithFixedDelay(this::checkPass, checkPassInterval / 2, checkPassInterval / 2, MILLISECONDS);
    }

    @Override
    public void register(URL url) {
        if (isConsumerSide(url)) {
            client.setKVValue(buildKVPathForConsumer(url), url.toFullString());
            return;
        }

        super.register(url);
    }

    @Override
    public void doRegister(URL url) {
        client.agentServiceRegister(buildService(url));
    }

    @Override
    public void unregister(URL url) {
        if (isConsumerSide(url)) {
            client.deleteKVValue(buildKVPathForConsumer(url));
            return;
        }

        super.unregister(url);
    }

    @Override
    public void doUnregister(URL url) {
        client.agentServiceDeregister(buildId(url));
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (isProviderSide(url)) {
            return;
        }

        super.subscribe(url, listener);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        String service = url.getServiceKey();
        Response<List<HealthService>> healthServices = getHealthServices(service, -1, buildWatchTimeout(url));
        Long index = healthServices.getConsulIndex();
        List<URL> urls = convert(healthServices.getValue());
        notify(url, listener, urls);

        ConsulNotifier notifier = notifiers.computeIfAbsent(url, k -> new ConsulNotifier(url, index));
        notifierExecutor.submit(notifier);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (isProviderSide(url)) {
            return;
        }

        super.unsubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        ConsulNotifier notifier = notifiers.remove(url);
        notifier.stop();
    }

    @Override
    public boolean isAvailable() {
        return client.getAgentSelf() != null;
    }

    @Override
    public void destroy() {
        super.destroy();
        notifierExecutor.shutdown();
        registerTimer.shutdown();
    }

    private Response<List<HealthService>> getHealthServices(String service, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(SERVICE_TAG)
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setPassing(true)
                .build();
        return client.getHealthServices(service, request);
    }

    private boolean isConsumerSide(URL url) {
        return url.getProtocol().equals(Constants.CONSUMER_PROTOCOL);
    }

    private boolean isProviderSide(URL url) {
        return url.getProtocol().equals(Constants.PROVIDER_PROTOCOL);
    }

    private List<URL> convert(List<HealthService> services) {
        return services.stream()
                .map(s -> s.getService().getMeta().get(URL_META_KEY))
                .map(URL::valueOf)
                .collect(Collectors.toList());
    }

    private void checkPass() {
        for (URL url : getRegistered()) {
            String checkId = buildId(url);
            try {
                client.agentCheckPass("service:" + checkId);
                if (logger.isDebugEnabled()) {
                    logger.debug("check pass for url: " + url + " with check id: " + checkId);
                }
            } catch (Throwable t) {
                logger.warn("fail to check pass for url: " + url + ", check id is: " + checkId);
            }
        }
    }

    private NewService buildService(URL url) {
        NewService service = new NewService();
        service.setAddress(url.getHost());
        service.setPort(url.getPort());
        service.setId(buildId(url));
        service.setName(url.getServiceKey());
        service.setCheck(buildCheck(url));
        service.setTags(Collections.singletonList(SERVICE_TAG));
        service.setMeta(Collections.singletonMap(URL_META_KEY, url.toFullString()));
        return service;
    }

    private String buildId(URL url) {
        // let's simply use url's hashcode to generate unique service id for now
        return Integer.toHexString(url.hashCode());
    }

    private NewService.Check buildCheck(URL url) {
        NewService.Check check = new NewService.Check();
        check.setTtl((checkPassInterval / 1000) + "s");
        return check;
    }

    private String buildKVPathForConsumer(URL url) {
        return rootPath + PATH_SEPARATOR + url.getServiceKey() + PATH_SEPARATOR + CONSUMERS_CATEGORY + PATH_SEPARATOR + url.getIp();
    }

    private int buildWatchTimeout(URL url) {
        return url.getParameter(WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT) / 1000;
    }

    private class ConsulNotifier implements Runnable {
        private URL url;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(URL url, long consulIndex) {
            this.url = url;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                String service = url.getServiceKey();
                Response<List<HealthService>> response = getHealthServices(service, consulIndex, buildWatchTimeout(url));
                Long currentIndex = response.getConsulIndex();
                if (currentIndex != null && currentIndex > consulIndex) {
                    consulIndex = currentIndex;
                    List<HealthService> services = response.getValue();
                    List<URL> urls = convert(services);
                    for (NotifyListener listener : getSubscribed().get(url)) {
                        doNotify(url, listener, urls);
                    }
                }
            }
        }

        void stop() {
            this.running = false;
        }
    }
}
