package org.apache.dubbo.configcenter.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.dubbo.common.Constants.CONFIG_NAMESPACE_KEY;

public class ConsulDynamicConfiguration implements DynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulDynamicConfiguration.class);

    private static final int DEFAULT_PORT = 8500;
    private static final int DEFAULT_WATCH_TIMEOUT = 2;

    private URL url;
    private String rootPath;
    private ConsulClient client;
    private ConcurrentMap<String, Set<ConfigurationListener>> listeners = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Long> consulIndexes = new ConcurrentHashMap<>();
    private ScheduledExecutorService watcher = newSingleThreadScheduledExecutor(
            new NamedThreadFactory("dubbo-consul-configuration-watcher", true));

    public ConsulDynamicConfiguration(URL url) {
        this.url = url;
        this.rootPath = "/" + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "/config";
        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
        watcher.scheduleWithFixedDelay(new ConsulKVWatcher(), DEFAULT_WATCH_TIMEOUT, DEFAULT_WATCH_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        Set<ConfigurationListener> listeners = this.listeners.computeIfAbsent(key, k -> new HashSet<>());
        listeners.add(listener);
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        Set<ConfigurationListener> listeners = this.listeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        if (StringUtils.isNotEmpty(group)) {
            key = group + "/" + key;
        } else {
            int i = key.lastIndexOf(".");
            key = key.substring(0, i) + "/" + key.substring(i + 1);
        }

        return (String) getInternalProperty(rootPath + "/" + key);
    }

    @Override
    public Object getInternalProperty(String key) {
        Long currentIndex = consulIndexes.computeIfAbsent(key, k -> -1L);
        Response<GetValue> response = client.getKVValue(key, new QueryParams(DEFAULT_WATCH_TIMEOUT, currentIndex));
        GetValue value = response.getValue();
        consulIndexes.put(key, response.getConsulIndex());
        return value != null ? value.getDecodedValue() : null;
    }

    private class ConsulKVWatcher implements Runnable {
        @Override
        public void run() {
            for (String key : listeners.keySet()) {
                Long currentIndex = consulIndexes.computeIfAbsent(key, k -> -1L);
                Response<GetValue> response = client.getKVValue(key, new QueryParams(DEFAULT_WATCH_TIMEOUT, currentIndex));
                if (response.getValue() == null) {
                    continue;
                }

                Long index = response.getConsulIndex();
                if (index == null || index.equals(currentIndex)) {
                    continue;
                }

                consulIndexes.put(key, index);
                ConfigChangeEvent event = new ConfigChangeEvent(key, response.getValue().getDecodedValue());
                for (ConfigurationListener listener : listeners.get(key)) {
                    listener.process(event);
                }
            }
        }
    }
}
