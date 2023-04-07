package org.apache.dubbo.metrics.data;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metrics.model.MetricsCategory;
import org.apache.dubbo.metrics.model.ServiceKeyMetric;
import org.apache.dubbo.metrics.model.container.LongContainer;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ServiceStatComposite {

    private final Map<MetricsKey, Map<ServiceKeyMetric, AtomicLong>> serviceNumStats = new ConcurrentHashMap<>();

    public void init(List<MetricsKey> serviceKeys) {
        if (CollectionUtils.isEmpty(serviceKeys)) {
            return;
        }
        serviceKeys.forEach(appKey -> serviceNumStats.put(appKey, new ConcurrentHashMap<>()));
    }

    public void incrementServiceKey(MetricsKey metricsKey, String applicationName, String serviceKey, int size) {
        if (!serviceNumStats.containsKey(metricsKey)) {
            return;
        }
        serviceNumStats.get(metricsKey).computeIfAbsent(new ServiceKeyMetric(applicationName, serviceKey), k -> new AtomicLong(0L)).getAndAdd(size);
    }

    public void setServiceKey(MetricsKey type, String applicationName, String serviceKey, int num) {
        if (!serviceNumStats.containsKey(type)) {
            return;
        }
        serviceNumStats.get(type).computeIfAbsent(new ServiceKeyMetric(applicationName, serviceKey), k -> new AtomicLong(0L)).set(num);
    }

    @SuppressWarnings({"rawtypes"})
    public List<GaugeMetricSample> export(MetricsCategory category) {
        List<GaugeMetricSample> list = new ArrayList<>();
        for (MetricsKey type : serviceNumStats.keySet()) {
            Map<ServiceKeyMetric, AtomicLong> stringAtomicLongMap = serviceNumStats.get(type);
            for (ServiceKeyMetric serviceKeyMetric : stringAtomicLongMap.keySet()) {
                list.add(new GaugeMetricSample<>(type, serviceKeyMetric.getTags(), category, stringAtomicLongMap, value -> value.get(serviceKeyMetric).get()));
            }
        }
        return list;
    }
}
